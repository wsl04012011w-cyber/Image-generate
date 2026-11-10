# Image Generate

Aplicativo Android nativo para download de modelos de difusão e geração local de imagens. Interface em Kotlin/Jetpack Compose, cliente HTTP para download e engine C++ por JNI baseada em [stable-diffusion.cpp](https://github.com/leejet/stable-diffusion.cpp).

## Funcionalidades implementadas

- Campo de prompt e controle de 4 a 20 steps (padrão: 8).
- URL HTTPS de modelo configurável e download para `filesDir/models`, no armazenamento privado do app.
- Estado de progresso e gravação temporária antes de substituir o modelo instalado.
- Seleção de CPU ou GPU/Vulkan; a camada JNI tenta Vulkan e repete a criação do contexto em CPU se o backend GPU não estiver disponível.
- Execução local, sem chamadas de rede durante a inferência.
- Workflow GitHub Actions para compilar APK debug em push para `main`, pull request e execução manual.

## Requisitos

- Android Studio recente, JDK 17, Android SDK 35, NDK 27.2 e CMake 3.22.1.
- Aparelho Android ARM64 recomendado; a memória disponível depende do modelo escolhido.
- Modelos SD-Turbo/SD 1.5 podem ocupar vários GB. Confira a licença e os termos do repositório de origem antes de baixar ou usar os pesos.

## Build

O build padrão compila o app e a biblioteca JNI de compatibilidade, mas **não baixa nem compila o engine de terceiros**. Assim o CI consegue validar o scaffold sem depender de uma compilação C++ longa. Para incluir stable-diffusion.cpp no APK:

```bash
./gradlew assembleDebug -PincludeStableDiffusion=true
```

Para habilitar também a compilação do backend Vulkan:

```bash
./gradlew assembleDebug -PincludeStableDiffusion=true -PenableVulkan=true
```

O primeiro build com engine precisa de internet para obter o código-fonte e seus submódulos. Depois de instalado o APK com engine e baixado o modelo, a inferência é executada localmente. A disponibilidade e o desempenho do Vulkan variam conforme o dispositivo, drivers e memória; a camada nativa tenta CPU quando a criação do contexto Vulkan falha.

> **Limitação atual:** o workflow gera um APK de debug funcional para validar a interface e o download. O APK padrão de CI não inclui os pesos nem o engine stable-diffusion.cpp; a geração só fica habilitada quando o engine nativo é compilado com `-PincludeStableDiffusion=true`. O suporte a modelos depende dos formatos e componentes exigidos pelo stable-diffusion.cpp. O app ainda não oferece conversão automática de checkpoints ou gerenciamento de arquivos auxiliares.

## Como usar

1. Instale o APK de debug.
2. Cole uma URL HTTPS direta para um arquivo de modelo compatível e toque em **Baixar modelo**.
3. Digite o prompt, selecione CPU ou GPU e ajuste os steps.
4. Toque em **Gerar imagem**. Após o download, a inferência não precisa de internet.

O endereço padrão aponta para o arquivo SD-Turbo no Hugging Face e pode ser alterado na tela. Alguns repositórios exigem aceite de licença ou autenticação e não disponibilizam links públicos diretos; nesses casos, use uma URL de download autorizada.

## GitHub Actions / APK

O workflow está em `.github/workflows/build-apk.yml`. Em cada push para `main` ou pull request direcionado a `main`, ele:

1. Faz checkout do repositório.
2. Configura JDK 17 e Android SDK/NDK/CMake.
3. Configura Gradle 8.10.2 com cache.
4. Executa `./gradlew assembleDebug`.
5. Publica `app/build/outputs/apk/debug/*.apk` como artefato por 14 dias.

Para baixar: abra a aba **Actions**, selecione a execução concluída e baixe o artefato **image-generate-debug-apk**. Também é possível iniciar o workflow manualmente.

## Licenças

O código do app é disponibilizado sob a licença MIT (consulte `LICENSE`). Os pesos dos modelos e o projeto stable-diffusion.cpp têm licenças próprias; elas não são substituídas pela licença deste app.
