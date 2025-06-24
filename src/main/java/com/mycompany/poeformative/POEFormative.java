/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.poeformative;

import java.awt.HeadlessException;
import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.json.JSONObject;
import java.nio.file.*;
import org.json.JSONException;


/**
 *
 * @author lab_services_student
 */

public class POEFormative {

    static class User {
        String firstName, lastName, username, password, phoneNumber;

        public User(String firstName, String lastName, String username, String password, String phoneNumber) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.password = password;
            this.phoneNumber = phoneNumber;
        }
    }

    static class SentMessage {
        String messageId, recipient, message;
        LocalDateTime timestamp;

        SentMessage(String messageId, String recipient, String message, LocalDateTime timestamp) {
            this.messageId = messageId;
            this.recipient = recipient;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    static boolean isLoggedIn = false;
    static int messagesSent = 0;
    static List<SentMessage> sentMessages = new ArrayList<>();
    static List<String> disregardedMessages = new ArrayList<>();
    static List<String> storedMessages = new ArrayList<>();
    static List<String> messageHashes = new ArrayList<>();
    static List<String> messageIds = new ArrayList<>();
    static User registeredUser;

    public static void main(String[] args) throws JSONException {
        showWelcomeSplash();
        registerUser();
        login();

        if (!isLoggedIn) return;

        JOptionPane.showMessageDialog(null, "🎉 Welcome to BUDCONNECT — Your Secure Chat App!");

        while (true) {
            String[] options = {"📨 Send Message", "📤 Send Stored Messages", "🧾 View Sent Messages", "📋 Reports Menu", "🚪 Quit"};
            int choice = JOptionPane.showOptionDialog(null, "📋 Select an action:", "💬 BUDCONNECT Main Menu",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            switch (choice) {
                case 0 -> sendMessages();
                case 1 -> sendStoredMessages();
                case 2 -> showSentMessages();
                case 3 -> reportMenu();
                case 4, JOptionPane.CLOSED_OPTION -> {
                    JOptionPane.showMessageDialog(null, "👋 Thank you for using BUDCONNECT. Goodbye!");
                    System.exit(0);
                }
                default -> System.exit(0);
            }
        }
    }

    public static void showWelcomeSplash() {
        JOptionPane.showMessageDialog(null, "💬 Welcome to BUDCONNECT\n🔐 Secure | ⚡ Fast | 📱 Mobile-Ready",
                "BBM Launch", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void registerUser() {
        String firstName = JOptionPane.showInputDialog("🧑 Enter First Name:");
        String lastName = JOptionPane.showInputDialog("🧑 Enter Last Name:");
        String username;
        do {
            username = JOptionPane.showInputDialog("🔐 Create a Username (use _ and max 5 characters):");
        } while (!isUsernameValid(username));
        String password;
        do {
            password = JOptionPane.showInputDialog("🔑 Create a Password (Min 8 chars, include uppercase, digit, special char):");
        } while (!isPasswordValid(password));
        String phoneNumber;
        do {
            phoneNumber = JOptionPane.showInputDialog("📱 Enter Phone Number (Format: +27123456789):");
        } while (!isPhoneNumberValid(phoneNumber));
        registeredUser = new User(firstName, lastName, username, password, phoneNumber);
    }

    public static void login() {
        String username = JOptionPane.showInputDialog("👤 Enter Username:");
        String password = JOptionPane.showInputDialog("🔒 Enter Password:");
        if (registeredUser.username.equals(username) && registeredUser.password.equals(password)) {
            isLoggedIn = true;
            JOptionPane.showMessageDialog(null, "✅ Login successful. Welcome, " + registeredUser.firstName + "!");
        } else {
            JOptionPane.showMessageDialog(null, "❌ Login failed.");
        }
    }

    public static void sendMessages() {
        if (!isLoggedIn) return;
        int messageCount = Integer.parseInt(JOptionPane.showInputDialog("📩 How many messages to send?"));
        for (int i = 0; i < messageCount; i++) {
            String recipient;
            do {
                recipient = JOptionPane.showInputDialog("📱 Recipient phone (e.g. +27123456789):");
            } while (!isValidRecipient(recipient));
            String message;
            do {
                message = JOptionPane.showInputDialog("📝 Message (1–250 characters):");
            } while (message == null || message.length() < 1 || message.length() > 250);
            String id = generateMessageId();
            LocalDateTime timestamp = LocalDateTime.now();
            String hash = generateMessageHash(id, message);
            messageHashes.add(hash);
            messageIds.add(id);

            String[] options = {"📨 Send", "❌ Disregard", "📥 Store for Later"};
            int action = JOptionPane.showOptionDialog(null,
                    "📱 To: " + recipient + "\n💬 Message: " + message + "\n🔐 Hash: " + hash,
                    "Choose Action", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            switch (action) {
                case 0 -> {
                    SentMessage sentMessage = new SentMessage(id, recipient, message, timestamp);
                    sentMessages.add(sentMessage);
                    saveMessageToFile(sentMessage);
                    messagesSent++;
                }
                case 1 -> disregardedMessages.add(message);
                case 2 -> storeMessageAsJSON(id, recipient, message, timestamp, hash);
                default -> JOptionPane.showMessageDialog(null, "Message disregarded.");
            }
        }
    }

    public static void sendStoredMessages() throws JSONException {
    List<String> storedLines = new ArrayList<>();
    try (Scanner scanner = new Scanner(new java.io.File("stored_messages.json"))) {
        while (scanner.hasNextLine()) {
            storedLines.add(scanner.nextLine());
        }
    } catch (IOException e) {
        JOptionPane.showMessageDialog(null, "⚠️ No stored messages file found.", "File Not Found", JOptionPane.WARNING_MESSAGE);
        return;
    }

    if (storedLines.isEmpty()) {
        JOptionPane.showMessageDialog(null, "📭 You don't have any stored messages to send.", "Empty Storage", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    DefaultListModel<String> listModel = new DefaultListModel<>();
    List<JSONObject> storedObjects = new ArrayList<>();

    for (String line : storedLines) {
        try {
            JSONObject obj = new JSONObject(line);
            storedObjects.add(obj);
            String preview = obj.getString("message");
            preview = preview.length() > 30 ? preview.substring(0, 30) + "..." : preview;
            listModel.addElement("📦 To: " + obj.getString("recipient") + " | " + preview);
        } catch (JSONException e) {
            // Skip malformed line
        }
    }

    if (listModel.isEmpty()) {
        JOptionPane.showMessageDialog(null, "⚠️ No valid stored messages found.", "Invalid Format", JOptionPane.WARNING_MESSAGE);
        return;
    }

    JList<String> messageList = new JList<>(listModel);
    messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(messageList);
    scrollPane.setPreferredSize(new java.awt.Dimension(450, 200));

    int selection = JOptionPane.showConfirmDialog(null, scrollPane, "📤 Select a stored message to send", JOptionPane.OK_CANCEL_OPTION);

    if (selection != JOptionPane.OK_OPTION || messageList.getSelectedIndex() == -1) {
        JOptionPane.showMessageDialog(null, "❌ No message selected.", "Cancelled", JOptionPane.WARNING_MESSAGE);
        return;
    }

    int index = messageList.getSelectedIndex();
    JSONObject selectedObj = storedObjects.get(index);
    try {
        String id = selectedObj.getString("id");
        String recipient = selectedObj.getString("recipient");
        String message = selectedObj.getString("message");
        LocalDateTime timestamp = LocalDateTime.parse(selectedObj.getString("timestamp"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        SentMessage sent = new SentMessage(id, recipient, message, timestamp);
        sentMessages.add(sent);
        saveMessageToFile(sent);
        storedLines.remove(index);
        Files.write(Paths.get("stored_messages.json"), storedLines);

        JOptionPane.showMessageDialog(null, "✅ Stored message sent successfully to " + recipient + "!", "Message Sent", JOptionPane.INFORMATION_MESSAGE);
    } catch (HeadlessException | IOException | JSONException e) {
        JOptionPane.showMessageDialog(null, "❌ An error occurred while sending the message.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}

    public static void showSentMessages() {
        if (sentMessages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "📭 No messages sent yet.");
            return;
        }
        StringBuilder builder = new StringBuilder("📑 Sent Messages:\n\n");
        for (int i = 0; i < sentMessages.size(); i++) {
            SentMessage msg = sentMessages.get(i);
            builder.append("🔹 ").append(i + 1).append(" | To: ").append(msg.recipient)
                    .append("\nID: ").append(msg.messageId)
                    .append("\nTime: ").append(formatTimestamp(msg.timestamp))
                    .append("\nMessage: ").append(msg.message).append("\n\n");
        }
        JTextArea area = new JTextArea(builder.toString());
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new java.awt.Dimension(500, 400));
        JOptionPane.showMessageDialog(null, scroll, "🗂 Sent Messages", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void storeMessageAsJSON(String id, String recipient, String message, LocalDateTime time, String hash) {
        String json = String.format("{\"id\":\"%s\",\"recipient\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"hash\":\"%s\"}",
                id, recipient, message.replace("\"", "\\\""), formatTimestamp(time), hash);
        try (FileWriter file = new FileWriter("stored_messages.json", true)) {
            file.write(json + System.lineSeparator());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error storing message.");
        }
    }

    public static boolean isUsernameValid(String username) {
        return username != null && username.contains("_") && username.length() <= 5;
    }

    public static boolean isPasswordValid(String password) {
        return password != null && !password.equals(password.toLowerCase()) &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*()].*") &&
                password.length() >= 8;
    }

    public static boolean isPhoneNumberValid(String phone) {
        return phone != null && phone.matches("^\\+\\d{1,3}\\d{7,10}$");
    }

    public static boolean isValidRecipient(String phone) {
        return phone != null && phone.startsWith("+") && phone.length() <= 12;
    }

    public static String generateMessageId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String generateMessageHash(String id, String message) {
        String idPrefix = id.substring(0, 2);
        String[] words = message.split(" ");
        String first = words[0];
        String last = words.length > 1 ? words[words.length - 1] : first;
        return (idPrefix + ":" + message.length() + ":" + first + last).toUpperCase();
    }

    public static void saveMessageToFile(SentMessage message) {
        try (FileWriter writer = new FileWriter("sent_messages.txt", true)) {
            writer.write("ID: " + message.messageId + "\n");
            writer.write("To: " + message.recipient + "\n");
            writer.write("Time: " + formatTimestamp(message.timestamp) + "\n");
            writer.write("Message: " + message.message + "\n");
            writer.write("--------------------------------------------------\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving message.");
        }
    }
    
    public static void reportMenu() {
        String[] reportOptions = {
                "👤 Display sender/recipient of sent messages",
                "📏 Show longest message",
                "🔍 Search by Message ID",
                "📇 Search messages by recipient",
                "🗑 Delete message by hash",
                "📃 Full report"
        };
        int reportChoice = JOptionPane.showOptionDialog(null, "📊 Choose a report option:", "📋 Reports Menu",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, reportOptions, reportOptions[0]);
        switch (reportChoice) {
            case 0 -> showSendersAndRecipients();
            case 1 -> showLongestMessage();
            case 2 -> searchByMessageID();
            case 3 -> searchByRecipient();
            case 4 -> deleteByHash();
            case 5 -> showFullReport();
        }
    }

    public static void showSendersAndRecipients() {
        if (sentMessages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No sent messages.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (SentMessage m : sentMessages) {
            sb.append("Sender: ").append(registeredUser.username)
                    .append(" | Recipient: ").append(m.recipient).append("\n");
        }
        JOptionPane.showMessageDialog(null, sb.toString(), "👤 Sender/Recipient", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showLongestMessage() {
        if (sentMessages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No sent messages.");
            return;
        }
        SentMessage longest = sentMessages.get(0);
        for (SentMessage m : sentMessages) {
            if (m.message.length() > longest.message.length()) {
                longest = m;
            }
        }
        JOptionPane.showMessageDialog(null, "Longest Message to " + longest.recipient + ":\n" + longest.message);
    }

    public static void searchByMessageID() {
        String input = JOptionPane.showInputDialog("🔍 Enter message ID to search:");
        if (input == null || input.isBlank()) return;
        for (SentMessage m : sentMessages) {
            if (m.messageId.equalsIgnoreCase(input.trim())) {
                JOptionPane.showMessageDialog(null, "To: " + m.recipient + "\nMessage: " + m.message);
                return;
            }
        }
        JOptionPane.showMessageDialog(null, "❌ Message ID not found.");
    }

    public static void searchByRecipient() {
        String recipient = JOptionPane.showInputDialog("🔍 Enter recipient number:");
        if (recipient == null || recipient.isBlank()) return;
        StringBuilder sb = new StringBuilder();
        for (SentMessage m : sentMessages) {
            if (m.recipient.equals(recipient.trim())) {
                sb.append("ID: ").append(m.messageId)
                        .append("\nMessage: ").append(m.message)
                        .append("\n---\n");
            }
        }
        if (sb.length() == 0) {
            JOptionPane.showMessageDialog(null, "No messages found.");
        } else {
            JOptionPane.showMessageDialog(null, sb.toString());
        }
    }

    public static void deleteByHash() {
        String hash = JOptionPane.showInputDialog("🗑 Enter message hash to delete:");
        if (hash == null || hash.isBlank()) return;
        Iterator<SentMessage> iterator = sentMessages.iterator();
        while (iterator.hasNext()) {
            SentMessage m = iterator.next();
            String currentHash = generateMessageHash(m.messageId, m.message);
            if (currentHash.equalsIgnoreCase(hash.trim())) {
                iterator.remove();
                messageHashes.remove(currentHash);
                messageIds.remove(m.messageId);
                JOptionPane.showMessageDialog(null, "✅ Message deleted.");
                return;
            }
        }
        JOptionPane.showMessageDialog(null, "❌ Hash not found.");
    }

    public static void showFullReport() {
        if (sentMessages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No sent messages.");
            return;
        }
        StringBuilder sb = new StringBuilder("📋 Full Message Report:\n\n");
        for (SentMessage m : sentMessages) {
            sb.append("ID: ").append(m.messageId)
                    .append(" | Hash: ").append(generateMessageHash(m.messageId, m.message)).append("\n")
                    .append("To: ").append(m.recipient).append("\n")
                    .append("Msg: ").append(m.message).append("\n")
                    .append("Time: ").append(formatTimestamp(m.timestamp)).append("\n---\n");
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new java.awt.Dimension(500, 400));
        JOptionPane.showMessageDialog(null, scroll, "📋 Full Report", JOptionPane.INFORMATION_MESSAGE);
    }
}
