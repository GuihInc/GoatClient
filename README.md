# GoatClient

Client com foco em **PvP e survival para Minecraft Java Edition 26.3**, distribuído como mod Fabric, com launcher desktop independente. Interface escura com detalhes verdes, guarda-roupa, gerenciamento de contas e preferências vinculadas ao UUID da sessão ativa.

O foco dos dois modos é personalização, informações na tela e conveniência, sem modificar alcance, automatizar ataques ou incluir outras vantagens de combate. O menu de mods reúne FPS, coordenadas, teclas, direção, relógio e status de corrida; cada perfil de jogador pode ativar seus próprios módulos.

## Launcher para Linux, macOS e Windows

O módulo `launcher/` instala o jogo e suas dependências, permite escolher versões e aceita novos releases por catálogo local ou HTTPS, sem recompilar o executável. Usa sua logo, oferece perfil offline e ajuste de memória/Java.

Os pacotes Linux x64, Windows x64, macOS Intel e macOS Apple Silicon da versão **1.0.8** são publicados em [Releases](https://github.com/GuihInc/GoatClient/releases); o repositório é privado e exige acesso à conta GuihInc.

O launcher oferece login Microsoft para contas com Minecraft Java e perfil offline para mundos locais e servidores que aceitam esse tipo de sessão. [Instruções do launcher e publicação de novas versões](launcher/README.md).

## O que já funciona

- Dez capas próprias selecionáveis, com tecido sombreado e emblemas centrais limpos: Goat Original, Ember, Frost, Aurora, Void, Ocean, Sunset, Rose, Mint e Cherry. No jogo, a capa usa a física 3D nativa do Minecraft.
- Asas cosméticas com paleta combinando com a capa escolhida, aplicadas à textura de elytra quando o jogador equipa uma.
- Cinco pets 3D low-poly selecionáveis, presos ao ombro do jogador local (cabrito, raposa, abelha, slime e fantasminha), além de oito emotes locais rápidos.
- Opção Original para voltar à capa da conta, se houver, e botão para desligar os cosméticos do GoatClient.
- Menu pelo **Shift direito**, reconfigurável em Opções → Controles → Teclas → GoatClient.
- Editor de nick pelo **F5**, com a logo GoatClient opcional ao lado do nome exibido sobre seu personagem em terceira pessoa; é visual/local e não altera seu nick no servidor. F5 também continua trocando a perspectiva.
- Botão **GOAT** no menu principal e no menu de pausa.
- Logo fornecida pelo proprietário nos menus, no botão GOAT e no ícone do mod. O PNG original está em `src/main/resources/assets/goatclient/textures/gui/logo.png`, com transparência e proporções preservadas.
- Tela **Contas** com perfil offline por nick.
- Guarda-roupa com prévias das capas. Em janelas maiores, também há uma prévia 3D giratória da skin da conta.
- Menu de mods com FPS, coordenadas, teclas de movimento, direção, relógio e status de corrida, habilitados separadamente. As teclas exibidas acompanham seus controles personalizados.
- Salvamento por UUID em `config/goatclient.json`, com recuperação de JSON danificado e preservação de uma cópia `.broken-*`.
- O launcher compartilha o `options.txt` (controles, vídeo, áudio e acessibilidade) entre as versões; o arquivo principal fica na pasta de dados escolhida no GoatClient.

Os cosméticos desta versão aparecem **somente no seu dispositivo**. As asas aparecem ao equipar uma elytra; o pet 3D fica no ombro do jogador local e os emotes são notificações locais. Nada disso é enviado a outros jogadores. A skin, o nome e o UUID vêm da sessão ativa; você não precisa inserir senha no mod. As preferências não são sincronizadas entre máquinas. A prévia 3D do guarda-roupa mostra a skin; as capas são pré-visualizadas nos cartões.

## Instalar para jogar

1. Instale o Minecraft Java 26.3 e o [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.5 ou superior em seu launcher.
2. Baixe o [Fabric API](https://modrinth.com/mod/fabric-api/versions?g=26.3) **0.163.0+26.3** ou superior compatível com 26.3.
3. Coloque o Fabric API e `goatclient-0.2.6.jar` em `mods/` da instalação escolhida. Substitua o JAR antigo do GoatClient; mantenha apenas uma versão instalada. Não use o arquivo `-sources.jar` para jogar.
4. Inicie o perfil Fabric. Abra **GOAT**, equipe uma capa e entre em um mundo. Use **F5** para vê-la.

A opção Capa em Opções → Personalização da skin deve estar ligada. O Minecraft oculta a capa ao usar elytra e durante a invisibilidade, conforme o comportamento nativo.

## Conta offline

No menu principal, abra **GOAT → Contas**, digite um nick de 3 a 16 letras, números ou `_` e clique em **Usar perfil offline**. O client usa o UUID offline padrão do Minecraft. Esse perfil funciona em mundos locais e servidores que aceitam conexões offline; servidores com autenticação online exigem uma sessão Microsoft válida. O nick não é uma identidade verificada e diferencia maiúsculas de minúsculas.

O último nick offline fica em `config/goatclient.json`. O apelido do editor F5 e a logo ao lado dele afetam apenas a placa de nome renderizada no seu próprio client, não mudam a identidade offline nem o nome visto pelos outros. As capas e o HUD são associados ao UUID do perfil local; Realms fica indisponível no modo offline.

## Compilar e desenvolver

Instale um **JDK 25** e deixe `java` disponível no terminal ou configure `JAVA_HOME`. O Gradle Wrapper está incluído; você não precisa instalar o Gradle separadamente. A primeira execução baixa as dependências.

```sh
./gradlew build
```

O mod é gerado em `build/libs/goatclient-0.2.6.jar`. No Windows, use `gradlew.bat build`.

Para abrir uma instalação isolada de desenvolvimento:

```sh
./gradlew runClient
```

O diretório dessa instalação é `run/`. O jogador de desenvolvimento pode ter nome/UUID diferentes da sua conta real, e suas preferências ficam separadas.

Para executar os testes de persistência, identidade offline e autenticação Microsoft com servidor HTTP local simulado:

```sh
./gradlew test
```

Há também um teste gráfico opcional que abre o Minecraft, navega pelo guarda-roupa, menu de mods e contas, verifica a interface compacta, testa o pet 3D no ombro/emotes/asa, troca para offline, restaura o launcher, cria um mundo e valida a identidade offline e a aplicação das capas no jogador:

```sh
./gradlew runClientGameTest --args='--width 1440 --height 900'
```

Ele exige uma sessão gráfica e usa uma instalação separada em `build/run/clientGameTest/`. O mod de teste não é incluído no JAR de distribuição. As capturas ficam em `build/run/clientGameTest/screenshots/`.

Validação: compilação concluída, testes locais de autenticação/persistência aprovados e teste gráfico aprovado no Minecraft 26.3, incluindo troca offline, restauração do launcher, isolamento dos cosméticos e recuperação de uma troca malsucedida. O fluxo Microsoft usa o Application ID público do GoatClient; o login com uma conta real ainda precisa ser concluído no navegador. As imagens ficam no diretório de screenshots do teste gráfico.

Versões fixadas: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.163.0+26.3, Loom 1.17.21, Gradle 9.6.0 e Java 25. A versão 26.3 usa os nomes públicos das classes do Minecraft e não precisa de Yarn. Referência técnica: [Fabric para Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html).

## Organização

`src/main/java` contém o catálogo de cosméticos e a persistência. `src/client/java` contém os menus, o perfil offline, o HUD, as texturas e a integração de renderização. `src/test/java` testa identidade offline e persistência. `src/gametest` contém o teste gráfico de integração. `launcher/` contém o aplicativo desktop, instalador do jogo, catálogo de releases, empacotamento nativo e seus testes.

## Próxima etapa do produto

Os cosméticos desta versão são locais. Para que outros jogadores também vejam asas, pets ou emotes, seria necessário um serviço de sincronização por UUID e renderização compartilhada. O pet e os emotes não são enviados ao servidor. Para distribuição pública, faltam hospedar um catálogo confiável e assinar os executáveis.

Produto independente; não é um produto oficial da Mojang ou Microsoft e não é afiliado ao Lunar Client ou Badlion.
