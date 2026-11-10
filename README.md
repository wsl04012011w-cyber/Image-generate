# Image Generate

Aplicativo Android nativo para geração local de imagens por difusão. O projeto começa com a interface, download de modelos e pipeline de build; a integração de inferência nativa é opcional até que o engine seja compilado para a ABI do dispositivo.

## Estado do projeto

- Interface Kotlin + Jetpack Compose.
- Download retomável via OkHttp para armazenamento privado do app.
- Seleção CPU/GPU e fallback para CPU.
- Integração JNI preparada para stable-diffusion.cpp.
- GitHub Actions para APK debug.

Consulte as instruções completas abaixo após o scaffold inicial.
