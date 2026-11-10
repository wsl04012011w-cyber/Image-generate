#include <jni.h>
#include <string>
#include <vector>

#ifdef IMAGEGEN_WITH_SD
#include "stable-diffusion.h"
#endif

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_imagegenerate_NativeDiffusion_nativeIsAvailable(JNIEnv*, jobject) {
#ifdef IMAGEGEN_WITH_SD
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_imagegenerate_NativeDiffusion_nativeGenerate(
    JNIEnv* env, jobject, jstring model_path, jstring prompt, jint steps, jstring backend) {
#ifndef IMAGEGEN_WITH_SD
    (void)env; (void)model_path; (void)prompt; (void)steps; (void)backend;
    return nullptr;
#else
    const char* model = env->GetStringUTFChars(model_path, nullptr);
    const char* text = env->GetStringUTFChars(prompt, nullptr);
    const char* requested_backend = env->GetStringUTFChars(backend, nullptr);

    sd_ctx_params_t ctx_params;
    sd_ctx_params_init(&ctx_params);
    ctx_params.model_path = model;
    ctx_params.n_threads = sd_get_num_physical_cores();
    ctx_params.backend = requested_backend;
    sd_ctx_t* ctx = new_sd_ctx(&ctx_params);

    // Unsupported or unavailable GPU backends fall back to CPU.
    if (ctx == nullptr && std::string(requested_backend) != "cpu") {
        ctx_params.backend = "cpu";
        ctx = new_sd_ctx(&ctx_params);
    }

    jbyteArray output = nullptr;
    if (ctx != nullptr) {
        sd_img_gen_params_t gen;
        sd_img_gen_params_init(&gen);
        gen.prompt = text;
        gen.width = 512;
        gen.height = 512;
        gen.sample_params.sample_steps = steps;
        gen.sample_params.sample_method = EULER_A_SAMPLE_METHOD;

        sd_image_generation_result_t* result = nullptr;
        if (generate_image(ctx, &gen, &result) && result != nullptr && result->count > 0) {
            const sd_image_t& image = result->items[0].image;
            if (image.width == 512 && image.height == 512 && image.data != nullptr) {
                const int channels = static_cast<int>(image.channel);
                std::vector<jbyte> rgb(512 * 512 * 3);
                for (int i = 0; i < 512 * 512; ++i) {
                    rgb[i * 3] = static_cast<jbyte>(image.data[i * channels]);
                    rgb[i * 3 + 1] = static_cast<jbyte>(image.data[i * channels + 1]);
                    rgb[i * 3 + 2] = static_cast<jbyte>(image.data[i * channels + 2]);
                }
                output = env->NewByteArray(static_cast<jsize>(rgb.size()));
                if (output != nullptr) env->SetByteArrayRegion(output, 0, static_cast<jsize>(rgb.size()), rgb.data());
            }
            free_sd_image_generation_result(result);
        }
        free_sd_ctx(ctx);
    }

    env->ReleaseStringUTFChars(model_path, model);
    env->ReleaseStringUTFChars(prompt, text);
    env->ReleaseStringUTFChars(backend, requested_backend);
    return output;
#endif
}
