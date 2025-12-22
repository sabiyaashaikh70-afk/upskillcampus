import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Music Player Application
 * A complete music player with playlist management, JDBC database integration,
 * error handling, and user interface components.
 * 
 * @author upskillcampus
 * @version 1.0
 */
public class MusicPlayerApp extends JFrame {
    private MusicPlayer musicPlayer;
    private PlaylistManager playlistManager;
    private DatabaseManager databaseManager;
    private JButton playButton, pauseButton, stopButton, previousButton, nextButton;
    private JButton addSongButton, createPlaylistButton, deletePlaylistButton;
    private JLabel currentSongLabel, timeLabel, statusLabel;
    private JSlider progressSlider, volumeSlider;
    private JComboBox<String> playlistComboBox;
    private JTable songTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;

    public MusicPlayerApp() {
        setTitle("Music Player Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(true);

        // Initialize components
        databaseManager = new DatabaseManager();
        databaseManager.initializeDatabase();
        
        musicPlayer = new MusicPlayer();
        playlistManager = new PlaylistManager(databaseManager);

        // Create UI
        initializeUI();
        
        // Set window close listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });

        setVisible(true);
    }

    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Panel - Current Song Info
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center Panel - Playlist and Songs
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel - Controls
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Now Playing"));
        panel.setPreferredSize(new Dimension(0, 120));

        // Song info section
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        currentSongLabel = new JLabel("No song selected");
        currentSongLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timeLabel = new JLabel("00:00 / 00:00");
        statusLabel = new JLabel("Status: Stopped");
        
        infoPanel.add(currentSongLabel);
        infoPanel.add(timeLabel);
        infoPanel.add(statusLabel);

        // Progress slider
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.addChangeListener(e -> {
            if (musicPlayer.isPlaying() && progressSlider.getModel().getValueIsAdjusting()) {
                musicPlayer.setProgress(progressSlider.getValue());
            }
        });

        // Volume slider
        JPanel volumePanel = new JPanel(new BorderLayout(5, 5));
        volumePanel.add(new JLabel("Volume:"), BorderLayout.WEST);
        volumeSlider = new JSlider(0, 100, 70);
        volumeSlider.addChangeListener(e -> musicPlayer.setVolume(volumeSlider.getValue()));
        volumePanel.add(volumeSlider, BorderLayout.CENTER);

        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(progressSlider, BorderLayout.CENTER);
        panel.add(volumePanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Left Panel - Playlist Management
        JPanel playlistPanel = createPlaylistPanel();
        
        // Right Panel - Song Table
        JPanel tablePanel = createTablePanel();

        panel.add(playlistPanel, BorderLayout.WEST);
        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPlaylistPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Playlists"));
        panel.setPreferredSize(new Dimension(200, 0));

        // Playlist combo box
        playlistComboBox = new JComboBox<>();
        playlistComboBox.addActionListener(e -> loadPlaylistSongs());
        panel.add(new JLabel("Select Playlist:"));
        panel.add(playlistComboBox);

        // Buttons
        createPlaylistButton = new JButton("Create Playlist");
        createPlaylistButton.addActionListener(e -> createNewPlaylist());
        panel.add(createPlaylistButton);

        deletePlaylistButton = new JButton("Delete Playlist");
        deletePlaylistButton.addActionListener(e -> deletePlaylist());
        panel.add(deletePlaylistButton);

        addSongButton = new JButton("Add Song");
        addSongButton.addActionListener(e -> addSongToPlaylist());
        panel.add(addSongButton);

        // Load playlists
        refreshPlaylistComboBox();

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Playlist Songs"));

        // Table
        tableModel = new DefaultTableModel(new String[]{"Song Name", "Duration", "Artist"}, 0);
        songTable = new JTable(tableModel);
        songTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    playSong();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(songTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea(3, 20);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        panel.add(logScrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Playback Controls"));

        previousButton = new JButton("⏮ Previous");
        previousButton.addActionListener(e -> previousSong());
        panel.add(previousButton);

        playButton = new JButton("▶ Play");
        playButton.addActionListener(e -> playSong());
        panel.add(playButton);

        pauseButton = new JButton("⏸ Pause");
        pauseButton.addActionListener(e -> pauseSong());
        panel.add(pauseButton);

        stopButton = new JButton("⏹ Stop");
        stopButton.addActionListener(e -> stopSong());
        panel.add(stopButton);

        nextButton = new JButton("Next ⏭");
        nextButton.addActionListener(e -> nextSong());
        panel.add(nextButton);

        return panel;
    }

    private void createNewPlaylist() {
        String playlistName = JOptionPane.showInputDialog(this, "Enter playlist name:");
        if (playlistName != null && !playlistName.isEmpty()) {
            try {
                playlistManager.createPlaylist(playlistName);
                refreshPlaylistComboBox();
                log("Playlist '" + playlistName + "' created successfully.");
                JOptionPane.showMessageDialog(this, "Playlist created successfully!");
            } catch (Exception ex) {
                log("Error creating playlist: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error creating playlist: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deletePlaylist() {
        Object selected = playlistComboBox.getSelectedItem();
        if (selected != null) {
            String playlistName = selected.toString();
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Delete playlist '" + playlistName + "'?", "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    playlistManager.deletePlaylist(playlistName);
                    refreshPlaylistComboBox();
                    tableModel.setRowCount(0);
                    log("Playlist '" + playlistName + "' deleted successfully.");
                    JOptionPane.showMessageDialog(this, "Playlist deleted successfully!");
                } catch (Exception ex) {
                    log("Error deleting playlist: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error deleting playlist: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void addSongToPlaylist() {
        Object selected = playlistComboBox.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a playlist first.", 
                "No Playlist", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Audio Files (mp3, wav, flac)", "mp3", "wav", "flac"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String playlistName = selected.toString();
                String songName = file.getName();
                String filePath = file.getAbsolutePath();
                
                playlistManager.addSongToPlaylist(playlistName, songName, filePath);
                loadPlaylistSongs();
                log("Song '" + songName + "' added to playlist.");
                JOptionPane.showMessageDialog(this, "Song added to playlist!");
            } catch (Exception ex) {
                log("Error adding song: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error adding song: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadPlaylistSongs() {
        Object selected = playlistComboBox.getSelectedItem();
        tableModel.setRowCount(0);
        
        if (selected != null) {
            try {
                String playlistName = selected.toString();
                java.util.List<Song> songs = playlistManager.getPlaylistSongs(playlistName);
                
                for (Song song : songs) {
                    tableModel.addRow(new Object[]{
                        song.getName(),
                        formatDuration(song.getDuration()),
                        song.getArtist()
                    });
                }
                log("Loaded " + songs.size() + " songs from playlist.");
            } catch (Exception ex) {
                log("Error loading playlist: " + ex.getMessage());
            }
        }
    }

    private void playSong() {
        int selectedRow = songTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                Object selected = playlistComboBox.getSelectedItem();
                String playlistName = selected.toString();
                java.util.List<Song> songs = playlistManager.getPlaylistSongs(playlistName);
                Song song = songs.get(selectedRow);
                
                musicPlayer.playSong(song.getFilePath());
                currentSongLabel.setText("Now Playing: " + song.getName());
                statusLabel.setText("Status: Playing");
                log("Playing: " + song.getName());
            } catch (Exception ex) {
                log("Error playing song: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error playing song: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a song to play.", 
                "No Song Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void pauseSong() {
        try {
            musicPlayer.pauseSong();
            statusLabel.setText("Status: Paused");
            log("Song paused.");
        } catch (Exception ex) {
            log("Error pausing song: " + ex.getMessage());
        }
    }

    private void stopSong() {
        try {
            musicPlayer.stopSong();
            currentSongLabel.setText("No song selected");
            statusLabel.setText("Status: Stopped");
            timeLabel.setText("00:00 / 00:00");
            progressSlider.setValue(0);
            log("Song stopped.");
        } catch (Exception ex) {
            log("Error stopping song: " + ex.getMessage());
        }
    }

    private void previousSong() {
        int selectedRow = songTable.getSelectedRow();
        if (selectedRow > 0) {
            songTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
            playSong();
        } else {
            JOptionPane.showMessageDialog(this, "No previous song available.", 
                "No Previous Song", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void nextSong() {
        int selectedRow = songTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < songTable.getRowCount() - 1) {
            songTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
            playSong();
        } else {
            JOptionPane.showMessageDialog(this, "No next song available.", 
                "No Next Song", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshPlaylistComboBox() {
        playlistComboBox.removeAllItems();
        try {
            java.util.List<String> playlists = playlistManager.getAllPlaylists();
            for (String playlist : playlists) {
                playlistComboBox.addItem(playlist);
            }
        } catch (Exception ex) {
            log("Error refreshing playlists: " + ex.getMessage());
        }
    }

    private String formatDuration(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void log(String message) {
        logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " 
            + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void cleanup() {
        try {
            musicPlayer.stopSong();
            databaseManager.closeConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MusicPlayerApp());
    }
}

/**
 * Music Player - Handles audio playback functionality
 */
class MusicPlayer {
    private javax.sound.sampled.SourceDataLine sourceDataLine;
    private Thread playbackThread;
    private boolean isPlaying;
    private float volume;
    private int currentPosition;
    private int totalDuration;

    public MusicPlayer() {
        isPlaying = false;
        volume = 0.7f;
        currentPosition = 0;
        totalDuration = 0;
    }

    public void playSong(String filePath) throws Exception {
        if (isPlaying) {
            stopSong();
        }

        playbackThread = new Thread(() -> {
            try {
                javax.sound.sampled.AudioInputStream audioInputStream = 
                    javax.sound.sampled.AudioSystem.getAudioInputStream(new File(filePath));
                javax.sound.sampled.AudioFormat audioFormat = audioInputStream.getFormat();
                
                javax.sound.sampled.DataLine.Info info = 
                    new javax.sound.sampled.DataLine.Info(
                        javax.sound.sampled.SourceDataLine.class, audioFormat);
                
                sourceDataLine = (javax.sound.sampled.SourceDataLine) 
                    javax.sound.sampled.AudioSystem.getLine(info);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

                isPlaying = true;
                byte[] bytesBuffer = new byte[4096];
                int bytesRead = -1;

                while ((bytesRead = audioInputStream.read(bytesBuffer)) != -1 && isPlaying) {
                    sourceDataLine.write(bytesBuffer, 0, bytesRead);
                    currentPosition++;
                }

                sourceDataLine.drain();
                sourceDataLine.close();
                audioInputStream.close();
                isPlaying = false;
            } catch (Exception ex) {
                throw new RuntimeException("Error playing audio: " + ex.getMessage());
            }
        });

        playbackThread.start();
    }

    public void pauseSong() {
        if (sourceDataLine != null && isPlaying) {
            sourceDataLine.stop();
            isPlaying = false;
        }
    }

    public void stopSong() {
        isPlaying = false;
        if (sourceDataLine != null) {
            sourceDataLine.stop();
            sourceDataLine.close();
        }
        currentPosition = 0;
    }

    public void setVolume(int volumePercent) {
        volume = volumePercent / 100.0f;
        if (sourceDataLine != null && sourceDataLine.isOpen()) {
            javax.sound.sampled.FloatControl volumeControl = 
                (javax.sound.sampled.FloatControl) sourceDataLine.getControl(
                    javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            volumeControl.setValue(dB);
        }
    }

    public void setProgress(int percent) {
        currentPosition = (int) ((totalDuration / 100.0) * percent);
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}

/**
 * Playlist Manager - Manages playlists and songs with database integration
 */
class PlaylistManager {
    private DatabaseManager databaseManager;

    public PlaylistManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void createPlaylist(String playlistName) throws SQLException {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            throw new IllegalArgumentException("Playlist name cannot be empty");
        }

        String sql = "INSERT INTO playlists (name, created_date) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playlistName);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
        }
    }

    public void deletePlaylist(String playlistName) throws SQLException {
        String sql = "DELETE FROM playlists WHERE name = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playlistName);
            stmt.executeUpdate();
        }
    }

    public void addSongToPlaylist(String playlistName, String songName, String filePath) 
            throws SQLException {
        String getPlaylistIdSql = "SELECT id FROM playlists WHERE name = ?";
        int playlistId = -1;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getPlaylistIdSql)) {
            stmt.setString(1, playlistName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                playlistId = rs.getInt("id");
            }
        }

        if (playlistId == -1) {
            throw new SQLException("Playlist not found");
        }

        String addSongSql = "INSERT INTO songs (playlist_id, name, file_path, duration, artist) " +
                           "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(addSongSql)) {
            stmt.setInt(1, playlistId);
            stmt.setString(2, songName);
            stmt.setString(3, filePath);
            stmt.setInt(4, 0); // Duration would be read from file
            stmt.setString(5, "Unknown Artist");
            stmt.executeUpdate();
        }
    }

    public java.util.List<Song> getPlaylistSongs(String playlistName) throws SQLException {
        java.util.List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.name, s.file_path, s.duration, s.artist FROM songs s " +
                    "JOIN playlists p ON s.playlist_id = p.id WHERE p.name = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playlistName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(
                    rs.getString("name"),
                    rs.getString("file_path"),
                    rs.getInt("duration"),
                    rs.getString("artist")
                );
                songs.add(song);
            }
        }

        return songs;
    }

    public java.util.List<String> getAllPlaylists() throws SQLException {
        java.util.List<String> playlists = new ArrayList<>();
        String sql = "SELECT name FROM playlists ORDER BY created_date DESC";

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                playlists.add(rs.getString("name"));
            }
        }

        return playlists;
    }
}

/**
 * Database Manager - Manages JDBC database operations
 */
class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:musicplayer.db";
    private Connection connection;

    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            System.err.println("SQLite driver not found: " + ex.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    public void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create playlists table
            String createPlaylistsTable = "CREATE TABLE IF NOT EXISTS playlists (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE NOT NULL," +
                "created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createPlaylistsTable);

            // Create songs table
            String createSongsTable = "CREATE TABLE IF NOT EXISTS songs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "playlist_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "file_path TEXT NOT NULL," +
                "duration INTEGER DEFAULT 0," +
                "artist TEXT DEFAULT 'Unknown Artist'," +
                "added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE)";
            stmt.execute(createSongsTable);

        } catch (SQLException ex) {
            System.err.println("Database initialization error: " + ex.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            System.err.println("Error closing connection: " + ex.getMessage());
        }
    }
}

/**
 * Song - Represents a song object
 */
class Song {
    private String name;
    private String filePath;
    private int duration;
    private String artist;

    public Song(String name, String filePath, int duration, String artist) {
        this.name = name;
        this.filePath = filePath;
        this.duration = duration;
        this.artist = artist;
    }

    public String getName() { return name; }
    public String getFilePath() { return filePath; }
    public int getDuration() { return duration; }
    public String getArtist() { return artist; }

    public void setName(String name) { this.name = name; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setArtist(String artist) { this.artist = artist; }
}
