package dev.goatclient.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import dev.goatclient.auth.AuthSettings;
import dev.goatclient.auth.MicrosoftAccount;
import dev.goatclient.auth.MicrosoftAuth;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public final class LauncherFrame extends JFrame {
    private static final Color BACKGROUND = new Color(0x101820);
    private static final Color PANEL = new Color(0x19242E);
    private static final Color ACCENT = new Color(0x8BE9A4);
    private static final Color TEXT = new Color(0xF0F4F8);
    private final Path root;
    private final LauncherSettings settings;
    private final Downloader downloads;
    private final ReleaseCatalog catalog;
    private final GameRunner runner = new GameRunner();
    private final JComboBox<ReleaseCatalog.Release> versions = new JComboBox<>();
    private final JTextField nickname = new JTextField();
    private final JTextField catalogUrl = new JTextField();
    private final JTextField javaPath = new JTextField();
    private final JButton microsoftLogin = new JButton("Entrar com Microsoft");
    private final JButton offlineMode = new JButton("Usar perfil offline");
    private final JLabel accountStatus = new JLabel("Sessão offline");
    private final JSpinner memory;
    private final JButton play = new JButton("INSTALAR E JOGAR");
    private final JButton refresh = new JButton("Verificar versões");
    private final JButton importCatalog = new JButton("Importar catálogo...");
    private final JButton saveSettings = new JButton("Salvar configurações");
    private final JLabel dataDirectoryLabel = new JLabel();
    private final JButton cancel = new JButton("Cancelar");
    private final JLabel status = new JLabel("Pronto para jogar.");
    private final JProgressBar progressBar = new JProgressBar();
    private volatile String progress = "Pronto para jogar.";
    private Thread task;
    private boolean busy;
    private LaunchCommand.Account microsoftAccount;
    private int operation;
    private final Timer statusTimer;

    public LauncherFrame(Path root, LauncherSettings settings, Downloader downloads) throws Exception {
        super("GoatClient Launcher");
        this.root = root;
        this.settings = settings;
        this.downloads = downloads;
        catalog = new ReleaseCatalog(root);
        memory = new JSpinner(new SpinnerNumberModel(settings.memoryMb, 1024, 16384, 512));
        installTheme();
        setSize(940, 660);
        setMinimumSize(new Dimension(760, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND);
        Image image = ImageIO.read(LauncherFrame.class.getResource("/brand/logo.png"));
        setIconImage(image);
        JPanel header = panel(new BorderLayout(18, 0));
        header.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));
        header.add(new JLabel(new javax.swing.ImageIcon(image.getScaledInstance(66, 66, Image.SCALE_SMOOTH))), BorderLayout.WEST);
        JPanel heading = vertical();
        JLabel title = new JLabel("GOATCLIENT");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        title.setForeground(ACCENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(8));
        heading.add(new JLabel("PvP e Survival. Seu perfil. Seu estilo."));
        header.add(heading, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 24, 12, 24));
        tabs.addTab("Jogar", gamePanel());
        tabs.addTab("Configurações", settingsPanel());
        add(tabs, BorderLayout.CENTER);

        JPanel bottom = vertical();
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 24, 20, 24));
        status.setForeground(TEXT);
        bottom.add(status);
        bottom.add(Box.createVerticalStrut(8));
        progressBar.setIndeterminate(false);
        bottom.add(progressBar);
        bottom.add(Box.createVerticalStrut(12));
        JPanel actions = panel(new BorderLayout(10, 0));
        play.setBackground(ACCENT);
        play.setForeground(BACKGROUND);
        play.setPreferredSize(new Dimension(200, 42));
        actions.add(play, BorderLayout.CENTER);
        actions.add(cancel, BorderLayout.EAST);
        cancel.setEnabled(false);
        bottom.add(actions);
        add(bottom, BorderLayout.SOUTH);

        nickname.setText(settings.nickname);
        catalogUrl.setText(settings.catalogUrl);
        javaPath.setText(settings.javaPath);
        loadVersions();
        play.addActionListener(event -> launch());
        refresh.addActionListener(event -> refreshVersions());
        importCatalog.addActionListener(event -> importVersions());
        saveSettings.addActionListener(event -> {
            try { save(); progress = "Configurações salvas."; }
            catch (Exception exception) { error(exception); }
        });
        microsoftLogin.addActionListener(event -> microsoftLogin());
        offlineMode.addActionListener(event -> {
            microsoftAccount = null;
            accountStatus.setText("Sessão offline");
            progress = "O launcher usará o perfil offline.";
        });
        cancel.addActionListener(event -> {
            if (runner.gameRunning() && JOptionPane.showConfirmDialog(this,
                    "Encerrar Minecraft agora? Salve seu mundo antes de continuar.", "Encerrar jogo",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            runner.cancel();
            if (task != null) task.interrupt();
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (busy && JOptionPane.showConfirmDialog(LauncherFrame.this,
                        runner.gameRunning() ? "Encerrar Minecraft e fechar o launcher? Salve seu mundo antes."
                                : "Cancelar a operação e fechar o launcher?", "Fechar GoatClient", JOptionPane.YES_NO_OPTION)
                        != JOptionPane.YES_OPTION) return;
                operation++;
                runner.cancel();
                if (task != null) task.interrupt();
                statusTimer.stop();
                dispose();
            }
        });
        statusTimer = new Timer(150, event -> {
            status.setText(progress);
            status.setToolTipText(progress);
            cancel.setText(runner.gameRunning() ? "Encerrar jogo" : "Cancelar");
        });
        statusTimer.start();
        SwingUtilities.updateComponentTreeUI(this);
    }

    @Override public void dispose() {
        if (statusTimer != null) statusTimer.stop();
        super.dispose();
    }

    private JPanel gamePanel() {
        JPanel page = panel(new GridLayout(1, 2, 22, 0));
        page.setBorder(BorderFactory.createEmptyBorder(22, 18, 18, 18));
        JPanel releases = vertical();
        releases.add(label("ESCOLHA SUA VERSÃO"));
        releases.add(Box.createVerticalStrut(14));
        versions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        releases.add(versions);
        releases.add(Box.createVerticalStrut(16));
        releases.add(note("O launcher instala o Minecraft, o Fabric e o GoatClient automaticamente. A primeira instalação precisa de internet."));
        releases.add(Box.createVerticalStrut(18));
        releases.add(refresh);
        releases.add(Box.createVerticalStrut(8));
        releases.add(importCatalog);
        releases.add(Box.createVerticalStrut(18));
        releases.add(note("As versões têm pastas independentes. Seus mundos e configurações ficam preservados ao instalar um novo release."));
        page.add(releases);
        JPanel profile = vertical();
        profile.add(label("SEU PERFIL"));
        profile.add(Box.createVerticalStrut(14));
        profile.add(new JLabel("Nick do perfil offline"));
        profile.add(Box.createVerticalStrut(8));
        nickname.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        profile.add(nickname);
        profile.add(Box.createVerticalStrut(14));
        profile.add(accountStatus);
        profile.add(Box.createVerticalStrut(10));
        profile.add(microsoftLogin);
        profile.add(Box.createVerticalStrut(8));
        profile.add(offlineMode);
        profile.add(Box.createVerticalStrut(12));
        profile.add(note("Microsoft: use sua conta com licença Minecraft Java. Offline: mundos locais e servidores que aceitam sessões offline."));
        page.add(profile);
        return page;
    }

    private JPanel settingsPanel() {
        JPanel panel = vertical();
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 14, 18));
        field(panel, "Memória do Minecraft (MB)", memory);
        field(panel, "Java personalizado (opcional; deixe vazio para seleção automática)", javaPath);
        field(panel, "Endereço HTTPS do catálogo de atualizações (opcional)", catalogUrl);
        panel.add(saveSettings);
        panel.add(Box.createVerticalStrut(12));
        JButton folder = new JButton("Abrir pasta do launcher");
        folder.addActionListener(event -> {
            try { Desktop.getDesktop().open(root.toFile()); }
            catch (Exception exception) { progress = "Pasta: " + root; }
        });
        panel.add(folder);
        panel.add(Box.createVerticalStrut(16));
        panel.add(new JLabel("Pasta atual dos dados e instalações:"));
        dataDirectoryLabel.setText(root.toString());
        dataDirectoryLabel.setToolTipText(root.toString());
        panel.add(dataDirectoryLabel);
        panel.add(Box.createVerticalStrut(8));
        JButton changeFolder = new JButton("Alterar diretório...");
        changeFolder.addActionListener(event -> changeDataDirectory());
        panel.add(changeFolder);
        return panel;
    }

    private void changeDataDirectory() {
        JFileChooser chooser = new JFileChooser(root.toFile());
        chooser.setDialogTitle("Escolher diretório do GoatClient");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        try {
            if (!java.nio.file.Files.isDirectory(selected)) throw new IllegalArgumentException("Escolha uma pasta existente.");
            new LauncherLocation(Platform.current()).save(selected);
            dataDirectoryLabel.setText(selected.toString());
            dataDirectoryLabel.setToolTipText(selected.toString());
            JOptionPane.showMessageDialog(this,
                    "O GoatClient usará esta pasta na próxima vez que for aberto.\n"
                            + "Os dados da pasta atual não serão movidos nem apagados.",
                    "Diretório atualizado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception exception) { error(exception); }
    }

    private void launch() {
        try {
            save();
            var release = (ReleaseCatalog.Release) versions.getSelectedItem();
            if (release == null) throw new IllegalStateException("Escolha uma versão para jogar.");
            LaunchCommand.Account account = microsoftAccount != null
                    && microsoftAccount.expiresAt().isAfter(java.time.Instant.now())
                    ? microsoftAccount : LaunchCommand.Account.offline(nickname.getText());
            if (microsoftAccount != null && !account.online()) {
                microsoftAccount = null;
                accountStatus.setText("Sessão expirada — offline");
            }
            work("Preparando instalação...", () -> runner.run(root, release, settings, account, downloads, true,
                    text -> progress = text), result -> {
                if (result != 0) error(new IllegalStateException("O Minecraft encerrou com erro. Veja launcher.log na pasta da versão."));
            });
        } catch (Exception exception) { error(exception); }
    }

    private void refreshVersions() {
        try {
            save();
            String url = settings.catalogUrl.strip();
            if (url.isBlank()) { loadVersions(); progress = "Catálogo local carregado. Você pode importar novas versões."; return; }
            URI source = URI.create(url);
            work("Verificando novas versões...", () -> { catalog.update(source, downloads); return null; }, ignored -> {
                try { loadVersions(); progress = "Catálogo atualizado."; }
                catch (Exception exception) { error(exception); }
            });
        } catch (Exception exception) { error(exception); }
    }

    private void microsoftLogin() {
        MicrosoftAuth auth = new MicrosoftAuth();
        work("Solicitando autorização Microsoft...", () -> {
            var code = auth.begin(AuthSettings.DEFAULT_MICROSOFT_CLIENT_ID);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(code.verificationUri());
                } catch (Exception ignored) { /* The URL and code remain visible below. */ }
                JOptionPane.showMessageDialog(this,
                        "A página de autorização foi aberta. Entre com a conta Microsoft que possui Minecraft Java e informe este código:\n\n"
                                + code.userCode() + "\n\nSe o navegador não abriu, acesse " + code.verificationUri(),
                        "Entrar com Microsoft", JOptionPane.INFORMATION_MESSAGE);
            });
            return auth.finish(code, text -> progress = text);
        }, account -> {
            microsoftAccount = LaunchCommand.Account.microsoft(account);
            accountStatus.setText("Microsoft: " + account.name());
            progress = "Conta Microsoft conectada. O token fica apenas na memória do launcher.";
        });
    }

    private void importVersions() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Importar catálogo de versões do GoatClient");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try { catalog.importFile(chooser.getSelectedFile().toPath()); loadVersions(); progress = "Versões importadas."; }
        catch (Exception exception) { error(exception); }
    }

    private void loadVersions() throws Exception {
        versions.removeAllItems();
        for (var release : catalog.load()) {
            versions.addItem(release);
            if (release.id().equals(settings.selectedRelease)) versions.setSelectedItem(release);
        }
    }

    private void save() throws Exception {
        settings.nickname = nickname.getText().strip();
        settings.catalogUrl = catalogUrl.getText().strip();
        settings.javaPath = javaPath.getText().strip();
        settings.memoryMb = (Integer) memory.getValue();
        var release = (ReleaseCatalog.Release) versions.getSelectedItem();
        if (release != null) settings.selectedRelease = release.id();
        settings.save(root);
    }

    private <T> void work(String text, Callable<T> operationTask, Consumer<T> success) {
        if (busy) return;
        int generation = ++operation;
        setBusy(true);
        progress = text;
        task = Thread.ofVirtual().name("goat-launcher-worker").start(() -> {
            T result = null;
            Exception failure = null;
            try { result = operationTask.call(); }
            catch (Exception exception) { failure = exception; }
            T value = result;
            Exception error = failure;
            SwingUtilities.invokeLater(() -> {
                if (generation != operation || !isDisplayable()) return;
                setBusy(false);
                if (error instanceof InterruptedException) progress = "Operação cancelada.";
                else if (error != null) error(error);
                else success.accept(value);
            });
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        for (var component : new javax.swing.JComponent[]{play, refresh, importCatalog, saveSettings, versions,
                nickname, microsoftLogin, offlineMode}) component.setEnabled(!value);
        cancel.setEnabled(value);
        progressBar.setIndeterminate(value);
    }

    private void error(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = "Não foi possível concluir a operação.";
        progress = message;
        JOptionPane.showMessageDialog(this, message, "GoatClient", JOptionPane.WARNING_MESSAGE);
    }

    private static JPanel vertical() {
        JPanel panel = panel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
    private static JPanel panel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(PANEL);
        panel.setForeground(TEXT);
        return panel;
    }
    private static JLabel label(String text) { JLabel label = new JLabel(text); label.setForeground(ACCENT); return label; }
    private static JTextArea note(String text) {
        JTextArea note = new JTextArea(text);
        note.setWrapStyleWord(true); note.setLineWrap(true); note.setEditable(false);
        note.setBackground(PANEL); note.setForeground(new Color(0xABB8C5)); note.setFocusable(false);
        return note;
    }
    private static void field(JPanel panel, String title, javax.swing.JComponent component) {
        panel.add(new JLabel(title)); panel.add(Box.createVerticalStrut(5));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); panel.add(component); panel.add(Box.createVerticalStrut(12));
    }
    private static void installTheme() {
        for (String key : new String[]{"Panel.background", "TabbedPane.background", "OptionPane.background"}) UIManager.put(key, PANEL);
        for (String key : new String[]{"Label.foreground", "Button.foreground", "TabbedPane.foreground", "OptionPane.messageForeground"}) UIManager.put(key, TEXT);
        UIManager.put("Button.background", new Color(0x2B3B48));
        UIManager.put("TextField.background", new Color(0x0F1821));
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", ACCENT);
        UIManager.put("ComboBox.background", new Color(0x2B3B48));
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ProgressBar.foreground", ACCENT);
    }
}
