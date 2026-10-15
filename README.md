# GoatClient

Client com foco em **PvP e survival para Minecraft Java Edition 26.3**, distribuído como mod Fabric, com launcher desktop independente. Interface escura com detalhes verdes, guarda-roupa, gerenciamento de contas e preferências vinculadas ao UUID da sessão ativa.

O foco dos dois modos é personalização, informações na tela e conveniência, sem modificar alcance, automatizar ataques ou incluir outras vantagens de combate. O HUD atual oferece FPS, coordenadas e teclas; ainda não há perfis de configuração específicos para PvP e survival.

## Launcher para Linux, macOS e Windows

O módulo `launcher/` instala o jogo e suas dependências, permite escolher versões e aceita novos releases por catálogo local ou HTTPS, sem recompilar o executável. Usa sua logo e oferece modo offline, login Microsoft preparado e ajuste de memória/Java.

O pacote **Linux x64** está em `launcher/build/distributions/GoatClientLauncher-1.0.0-linux-x64.zip`. Extraia tudo e abra `GoatClientLauncher/bin/GoatClientLauncher`. O workflow também gerou os ZIPs para Windows x64, macOS Intel e macOS Apple Silicon. Baixe-os na seção **Artifacts** desta [execução do GitHub Actions](https://github.com/GuihInc/GoatClient/actions/runs/38770091762); o repositório é privado e exige acesso à conta GuihInc.

O login Microsoft próprio ainda depende do Application ID que falta configurar. O launcher independente não reutiliza a sessão do launcher oficial; a opção de restauração descrita abaixo pertence ao mod em execução. [Instruções do launcher, compilação e publicação de novas versões](launcher/README.md).

## O que já funciona

- Três capas próprias: Goat Original, Ember e Frost, com animação nativa do Minecraft.
- Opção Original para voltar à capa da conta, se houver, e botão para desligar os cosméticos do GoatClient.
- Menu pelo **Shift direito**, reconfigurável em Opções → Controles → Teclas → GoatClient.
- Botão **GOAT** no menu principal e no menu de pausa.
- Logo fornecida pelo proprietário nos menus, no botão GOAT e no ícone do mod. O PNG original está em `src/main/resources/assets/goatclient/textures/gui/logo.png`, com transparência e proporções preservadas.
- Tela **Contas** com restauração da sessão do launcher, modo offline por nick e login Microsoft por código no navegador, quando o aplicativo estiver configurado.
- Guarda-roupa com prévias das capas. Em janelas maiores, também há uma prévia 3D giratória da skin da conta.
- HUD com FPS, coordenadas e teclas de movimento, habilitados separadamente. As teclas exibidas acompanham seus controles personalizados.
- Salvamento por UUID em `config/goatclient.json`, com recuperação de JSON danificado e preservação de uma cópia `.broken-*`.

Os cosméticos desta versão aparecem **somente no seu dispositivo**. A skin, o nome e o UUID vêm da sessão ativa; você não precisa inserir senha no mod. As preferências não são sincronizadas entre máquinas. A prévia 3D mostra a skin; as capas são pré-visualizadas nos cartões do guarda-roupa.

## Instalar para jogar

1. Instale o Minecraft Java 26.3 e o [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.5 ou superior em seu launcher.
2. Baixe o [Fabric API](https://modrinth.com/mod/fabric-api/versions?g=26.3) **0.163.0+26.3** ou superior compatível com 26.3.
3. Coloque o Fabric API e `goatclient-0.2.1.jar` em `mods/` da instalação escolhida. Substitua o JAR antigo do GoatClient; mantenha apenas uma versão instalada. Não use o arquivo `-sources.jar` para jogar.
4. Inicie o perfil Fabric. Abra **GOAT**, equipe uma capa e entre em um mundo. Use **F5** para vê-la.

A opção Capa em Opções → Personalização da skin deve estar ligada. O Minecraft oculta a capa ao usar elytra e durante a invisibilidade, conforme o comportamento nativo.

## Contas: original e offline

No menu principal, abra **GOAT → Contas**. A troca fica desabilitada enquanto você está em um mundo ou servidor.

- **Launcher:** reutiliza a sessão com que você iniciou o Minecraft. Para uma conta original, faça login Microsoft no seu launcher e inicie o perfil Fabric. **Usar conta do launcher** restaura essa mesma sessão após usar o modo offline, sem exigir um Application ID do GoatClient. Essa opção não faz uma nova autenticação nem converte uma sessão offline do launcher em conta original.
- **Offline:** digite um nick com 3 a 16 letras, números ou `_` e clique em **Entrar offline**. O client usa o UUID offline padrão do Minecraft para esse nick. Funciona em mundos locais e servidores que aceitam conexões offline; servidores com autenticação online exigem uma sessão Microsoft válida. O nick offline não é uma identidade verificada e diferencia maiúsculas de minúsculas.
- **Microsoft:** login próprio do GoatClient pelo navegador, com código temporário. Exige um **Application (client) ID** de um aplicativo público registrado e configurado para os serviços Microsoft/Xbox/Minecraft. Como esse ID ainda não foi fornecido para o projeto, essa opção não está pronta para login real; use **Launcher** para sua conta original por enquanto.

Quando o aplicativo Microsoft estiver disponível, informe seu ID público na aba Microsoft e clique em **Entrar com Microsoft**. Abra a página pelo botão **Abrir Microsoft** e informe o código mostrado. O fluxo segue Microsoft → Xbox Live → XSTS → Minecraft, verifica a licença Java e carrega o perfil antes de trocar a sessão. Recusa, expiração, falta de licença ou erro de rede preservam a conta atual. **Cancelar login** ou sair da tela cancela a tentativa. Referência do protocolo: [Microsoft device authorization](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code).

O GoatClient não usa o ID de aplicativos de terceiros, não solicita um client secret e não recebe sua senha. O ID público e o último nick offline ficam em `config/goatclient.json`; tokens ficam apenas na memória. O login Microsoft próprio precisa ser repetido após reiniciar ou quando sua sessão expirar. Ao reiniciar, o mod usa novamente a conta fornecida pelo launcher.

Capas e HUD ficam separados por UUID. Ao alternar entre a conta original e um nick offline, as preferências de cada perfil são preservadas. Na troca, o mod atualiza também as chaves de chat, permissões, serviços sociais e a identidade usada nos downloads de resource packs; Realms fica indisponível no modo offline.

## Compilar e desenvolver

Instale um **JDK 25** e deixe `java` disponível no terminal ou configure `JAVA_HOME`. O Gradle Wrapper está incluído; você não precisa instalar o Gradle separadamente. A primeira execução baixa as dependências.

```sh
./gradlew build
```

O mod é gerado em `build/libs/goatclient-0.2.1.jar`. No Windows, use `gradlew.bat build`.

Para abrir uma instalação isolada de desenvolvimento:

```sh
./gradlew runClient
```

O diretório dessa instalação é `run/`. O jogador de desenvolvimento pode ter nome/UUID diferentes da sua conta real, e suas preferências ficam separadas.

Para executar os testes de persistência, identidade offline e autenticação Microsoft com servidor HTTP local simulado:

```sh
./gradlew test
```

Há também um teste gráfico opcional que abre o Minecraft, navega pelo guarda-roupa, HUD e contas, verifica a interface compacta, troca para offline, restaura o launcher, cria um mundo e valida a identidade offline e a aplicação de todas as capas no jogador:

```sh
./gradlew runClientGameTest --args='--width 1440 --height 900'
```

Ele exige uma sessão gráfica e usa uma instalação separada em `build/run/clientGameTest/`. O mod de teste não é incluído no JAR de distribuição. As capturas ficam em `build/run/clientGameTest/screenshots/`.

Validação: compilação concluída, 18 testes locais de autenticação/persistência aprovados e teste gráfico aprovado no Minecraft 26.3, incluindo troca offline, restauração do launcher, isolamento dos cosméticos e recuperação de uma troca malsucedida. O login Microsoft com conta real não foi validado, pois depende do Application ID e da autorização do usuário no navegador. As imagens ficam no diretório de screenshots do teste gráfico.

Versões fixadas: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.163.0+26.3, Loom 1.17.21, Gradle 9.6.0 e Java 25. A versão 26.3 usa os nomes públicos das classes do Minecraft e não precisa de Yarn. Referência técnica: [Fabric para Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html).

## Organização

`src/main/java` contém o catálogo de capas, a persistência e o protocolo Microsoft independente da interface. `src/client/java` contém os menus, a troca de sessão, o HUD, as texturas e a integração de renderização. `src/test/java` testa autenticação, identidade offline e persistência. `src/gametest` contém o teste gráfico de integração. `launcher/` contém o aplicativo desktop, instalador do jogo, catálogo de releases, empacotamento nativo e seus testes.

## Próxima etapa do produto

Este MVP cobre o guarda-roupa local, a identificação pelo perfil autenticado e um launcher com catálogo extensível. Para a experiência social de um client como Lunar/Badlion, ainda será necessário implementar um serviço de sincronização de cosméticos por UUID, renderização dos cosméticos de outros usuários e novos modelos (asas, chapéus etc.). Para distribuição pública, faltam hospedar um catálogo confiável, configurar o aplicativo Microsoft e assinar os executáveis.

Produto independente; não é um produto oficial da Mojang ou Microsoft e não é afiliado ao Lunar Client ou Badlion.
