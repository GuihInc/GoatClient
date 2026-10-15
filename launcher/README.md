# GoatClient Launcher

Launcher desktop independente para o GoatClient, com foco em **PvP e survival**: logo própria, seleção de versões, instalação de Minecraft/Fabric/Fabric API, download automático do Java necessário e perfis offline ou Microsoft. Versão do launcher: **1.0.0**. Versão incluída do client: **0.2.1 para Minecraft 26.3**. O client atual oferece cosméticos e HUD; não inclui vantagens de combate ou perfis separados por modo de jogo.

## Abrir

Extraia **todo** o ZIP correspondente ao seu sistema e arquitetura. Mantenha as pastas que acompanham o executável; não copie apenas o binário.

- **Linux x64:** abra `GoatClientLauncher/bin/GoatClientLauncher`. Se o extrator não preservar a permissão, execute `chmod +x GoatClientLauncher/bin/GoatClientLauncher` na pasta extraída.
- **Windows x64:** abra `GoatClientLauncher/GoatClientLauncher.exe`.
- **macOS:** abra `GoatClientLauncher.app`. Use o pacote `arm64` para Apple Silicon ou `x64` para Intel.

O Java do launcher já acompanha o aplicativo. Na primeira instalação, ele baixa os arquivos oficiais do jogo, o Fabric, o Fabric API e, se necessário, um JRE Temurin da Adoptium. Não é necessário instalar Java manualmente para usar o pacote nativo. A conexão com a internet é necessária para a instalação inicial, novas versões e login Microsoft. O sistema também precisa das bibliotecas gráficas e dos drivers exigidos pelo Minecraft.

Em **Jogar**, escolha a versão, informe seu nick na aba **Offline** e clique em **Instalar e jogar**. Em **Configurações**, ajuste a memória ou selecione um executável Java próprio. O padrão é 2 GB para o jogo, independentemente da memória usada pelo launcher. O botão de cancelamento pede confirmação antes de encerrar um jogo aberto; salve seu mundo primeiro.

Estes pacotes são aplicativos portáveis, não instaladores MSI/DMG/DEB. Ainda não há assinatura de distribuição Windows nem assinatura/notarização Apple; o sistema pode apresentar avisos. Para distribuição pública, configure a assinatura apropriada.

## Contas

**Offline:** disponível agora, para mundos locais e servidores que aceitam sessões offline. Não autentica propriedade de uma conta e não permite entrar em servidores que exigem login Microsoft válido.

**Microsoft:** o fluxo por código no navegador está implementado, mas depende de um **Application (client) ID público próprio**, configurado para o fluxo Microsoft/Xbox/Minecraft. O projeto ainda não possui esse ID e o login real não foi validado. Quando estiver disponível, informe-o em Configurações. O launcher verifica a licença Java e o perfil antes de iniciar uma sessão online. Não usa IDs emprestados, não solicita client secret e não recebe sua senha. Referência: [protocolo oficial de código de dispositivo](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code).

Este launcher independente **não importa a sessão do launcher oficial**. A opção **Launcher** existente dentro do mod apenas restaura a sessão com que o Minecraft foi iniciado. Enquanto faltar o Application ID, uma conta original pode continuar usando o mod pelo launcher oficial com Fabric.

Tokens não são salvos nas preferências. A sessão Microsoft fica em memória e deve ser refeita após reiniciar ou expirar. Para iniciar o jogo, os argumentos são colocados temporariamente em um arquivo restrito no diretório da instância; ele é removido ao encerrar normalmente. Uma interrupção abrupta do launcher ou do sistema pode impedir essa limpeza. Não compartilhe a pasta de dados durante uma sessão autenticada.

## Adicionar versões futuras

O catálogo é separado do executável. Uma nova versão compatível do client pode aparecer no mesmo launcher sem reinstalá-lo.

1. Atualize o código do mod para a versão desejada do Minecraft e ajuste `minecraft_version`, `loader_version`, `fabric_api_version` e `mod_version` em `gradle.properties`. Mudar apenas os números não garante compatibilidade: compile e teste o mod na nova versão.
2. Com JDK 25 disponível, execute na raiz do projeto:

   ```sh
   ./gradlew test :launcher:test :launcher:exportCatalog
   ```

   No Windows, use `gradlew.bat` no lugar de `./gradlew`.

3. O resultado fica em `launcher/build/release/`: `catalog.json` e `releases/goatclient-<versão>.jar`. O catálogo já inclui as versões das dependências e o SHA-256 do JAR.
4. Para uso local, distribua essa pasta completa. No launcher, clique em **Importar catálogo...** e escolha seu `catalog.json`. Caminhos relativos são resolvidos a partir desse arquivo; mantenha o JAR acessível até a primeira instalação.
5. Para distribuição online, publique o conteúdo da pasta em um servidor HTTPS de sua confiança. Configure a URL completa de `catalog.json` em **Configurações**, salve e clique em **Verificar versões**. A atualização do catálogo não instala nem executa versões automaticamente: o usuário escolhe e clica em jogar.

Para oferecer várias versões no mesmo catálogo publicado, reúna suas entradas no array `releases` e mantenha os JARs anteriores disponíveis. A exportação gera a versão atual do projeto, não um histórico automático. O launcher mescla versões importadas com as já conhecidas.

Cada entrada tem `id`, `name`, `minecraft`, `loader`, `fabricApi` e `client` com `url` e `sha256`. Use um **ID único e imutável para cada release**: o ID determina a pasta da instância. Reutilizar um ID atualiza os arquivos gerenciados dentro da mesma instância. URLs do catálogo remoto e dos JARs devem ser HTTPS; catálogos remotos não podem apontar para arquivos locais. O SHA-256 detecta alterações/corrupção, mas não substitui a confiança no responsável pelo catálogo. Importe apenas catálogos confiáveis: os JARs são código executável.

O modelo é voltado a versões modernas do Minecraft com Fabric. Uma mudança futura no formato de metadados ou no protocolo de autenticação poderá exigir atualização do próprio launcher. Não há hospedagem de catálogo, publicação automática ou autoatualização do executável configurada neste projeto.

## Dados e preservação dos mundos

- Linux: `$XDG_DATA_HOME/GoatClient` ou `~/.local/share/GoatClient`.
- Windows: `%APPDATA%\GoatClient`.
- macOS: `~/Library/Application Support/GoatClient`.

Cada versão usa `instances/<id>/`, com `saves/`, `mods/`, configurações e `launcher.log`. Assets, bibliotecas e runtimes são compartilhados dentro da pasta de dados. O launcher substitui somente seus JARs gerenciados em `mods/goatclient.jar` e `mods/fabric-api.jar`; não apaga mundos ou outros mods. A instalação de um release novo não migra mundos antigos automaticamente. Faça backup antes de transferir mundos entre versões do Minecraft.

## Compilar os executáveis

Com um **JDK 25** disponível, execute no sistema de destino:

```sh
./gradlew test :launcher:test :launcher:packageLauncher
```

No Windows: `gradlew.bat test :launcher:test :launcher:packageLauncher`.

O ZIP aparece em `launcher/build/distributions/GoatClientLauncher-1.0.0-<sistema>-<arquitetura>.zip`. O aplicativo usa `jpackage` e inclui seu próprio runtime. O empacotamento nativo deve rodar em cada sistema de destino, conforme a [documentação do JDK](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-overview.html).

O workflow `.github/workflows/launcher.yml` está preparado para Linux x64, Windows x64, macOS ARM64 e macOS Intel. Depois de colocar este projeto em um repositório GitHub, execute **Build GoatClient Launcher** em Actions ou envie uma tag `launcher-v*`. Os ZIPs ficam nos artefatos da execução; não são publicados automaticamente em Releases. Os ambientes vêm das [imagens oficiais do GitHub Actions](https://github.com/actions/runner-images).

**Validado neste ambiente:** 8 testes unitários do launcher, instalação completa de Minecraft 26.3 com Fabric/client, download e extração do JRE 25 e início real do jogo em Linux x64. O pacote Linux foi gerado localmente. Windows e macOS ainda precisam executar seu workflow nativo e ser validados nesses sistemas. O login Microsoft real depende do Application ID ausente.

Para executar o launcher a partir do código:

```sh
./gradlew :launcher:run
```

Para verificações sem abrir uma conta ou alterar sua instalação normal:

```sh
java -jar launcher/build/jpackage-input/goatclient-launcher.jar --self-test
java -jar launcher/build/jpackage-input/goatclient-launcher.jar --prepare --data-dir ./launcher/build/smoke-data
```

O JAR independente exige Java 25; os pacotes nativos não. `--prepare` baixa e instala, mas não inicia o jogo. `--data-dir` mantém os testes isolados. `--ui-smoke-test caminho.png` abre a interface, salva uma imagem apenas do conteúdo da própria janela e a fecha; exige uma sessão gráfica.

Produto independente; não é oficial nem afiliado à Mojang ou Microsoft.
