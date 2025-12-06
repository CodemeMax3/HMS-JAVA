import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
// Main Class
public class HospitalManagementSystem {

    // --- Main Application Frame and Panel Container ---
    private static JFrame mainFrame;
    private static JPanel mainPanel;
    private static CardLayout cardLayout;

    // --- User Session Management ---
    private static String currentAdminUser;
    private static Doctor currentDoctorUser;

    // --- Database Manager Instance ---
    private static final DatabaseManager dbManager = new DatabaseManager();
    private static TempPatientData tempPatientData;
    // --- UI Design and Color Palette ---
    public static final Color COLOR_PRIMARY = new Color(30, 136, 229); // A modern, friendly blue
    public static final Color COLOR_SECONDARY = new Color(25, 118, 210); // A slightly darker blue for accents
    public static final Color COLOR_BACKGROUND = new Color(244, 246, 249); // A light, clean grey
    public static final Color COLOR_FONT_LIGHT = Color.WHITE;
    public static final Color COLOR_FONT_DARK = new Color(51, 51, 51);
    public static final Color COLOR_SUCCESS = new Color(46, 125, 50);
    public static final Color COLOR_DANGER = new Color(211, 47, 47);
    // --- Main Page Background Image Path ---
    private static final String BACKGROUND_IMAGE_PATH = "https://ik.imagekit.io/1lb1vkk2o/unnamed.png?updatedAt=1758405338521";

    /**
     * The main entry point of the application.
     * Initializes the main frame and sets up the user interface.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            mainFrame = new JFrame("SALVE Memorial Hospital - Management System");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setSize(1280, 800);
            mainFrame.setLocationRelativeTo(null);

            // Set up background panel (keep existing background code)
            try {
                URL imageUrl = new URI(BACKGROUND_IMAGE_PATH).toURL();
                BufferedImage backgroundImage = ImageIO.read(imageUrl);
                if (backgroundImage == null) {
                    throw new IOException("Failed to load image from URL.");
                }
                BackgroundPanel backgroundPanel = new BackgroundPanel(backgroundImage);
                mainFrame.setContentPane(backgroundPanel);
            } catch (IOException | URISyntaxException e) {
                System.err.println("Could not load background image: " + e.getMessage());
                mainFrame.getContentPane().setBackground(COLOR_BACKGROUND);
            }

            cardLayout = new CardLayout();
            mainPanel = new JPanel(cardLayout);
            mainPanel.setOpaque(false);

            // Add ALL pages including the new ones
            mainPanel.add(new MainPage(), "MainPage");                  // Default landing page
            mainPanel.add(new PublicMainPage(), "PublicMainPage");     // Public interface
            mainPanel.add(new HospitalMainPage(), "HospitalMainPage"); // Staff interface
            mainPanel.add(new LoginPage(), "LoginPage");
            mainPanel.add(new AdminLoginPage(), "AdminLoginPage");
            mainPanel.add(new DoctorLoginPage(), "DoctorLoginPage");
            mainPanel.add(new PatientLoginPage(), "PatientLoginPage");
            mainPanel.add(new AppointmentBookingPage1(), "AppointmentBookingPage1");
            mainPanel.add(new EnhancedOnlinePaymentPage(), "OnlinePaymentPage");
            mainPanel.add(new OnlinePharmacyPage(), "OnlinePharmacyPage");
            mainPanel.add(new PatientMedicinePage(), "PatientMedicinePage");
            mainPanel.add(new PharmacyStockPage(), "PharmacyStockPage");
            mainPanel.add(new DoctorScheduleImportPage(), "DoctorScheduleImportPage");
            mainFrame.getContentPane().add(mainPanel);
            mainFrame.setVisible(true);

            // Auto-route based on IP (optional)
            printNetworkInfo();
            autoRouteBasedOnIP();
        });
    }
    public static void showConsultationPanel(Doctor doctor, Patient patient, Consultation consultation) {
        ConsultationPanel consultationPanel = new ConsultationPanel(doctor, patient, consultation);
        mainPanel.add(consultationPanel, "ConsultationPanel");
        cardLayout.show(mainPanel, "ConsultationPanel");
    }
    /**
     * Navigates to a specified page within the application.
     *
     * @param panelName The name of the JPanel to display.
     */
    public static void showPage(String panelName) {
        JPanel newPage;
        // For pages that need user data, create a new instance
        switch (panelName) {
            case "AdminPage":
                newPage = new AdminPage(currentAdminUser);
                break;
            case "DoctorPage":
                newPage = new DoctorPage(currentDoctorUser);
                break;
            case "DoctorStatusPage":
                newPage = new DoctorStatusPage();
                break;
            case "DoctorManagementPage":
                newPage = new DoctorManagementPage();
                break;
            case "AppointmentsViewPageAdmin":
                newPage = new AppointmentsViewPage(true, null);
                break;
            case "PatientDetailsPageAdmin":
                newPage = new PatientDetailsPage(true, null);
                break;
            case "UserManagementPage":
                newPage = new UserManagementPage();
                break;
            case "PatientMedicinePage":      // NEW
                newPage = new PatientMedicinePage();
                break;
            case "PharmacyStockPage":        // NEW
                newPage = new PharmacyStockPage();
                break;
            case "DoctorScheduleImportPage":
                newPage = new DoctorScheduleImportPage();
                break;
            default:
                // For static pages, just show them
                cardLayout.show(mainPanel, panelName);
                return;
        }
        mainPanel.add(newPage, panelName);
        cardLayout.show(mainPanel, panelName);
    }

    public static void showAddressPage(Map<Medicine, Integer> cart) {
        AddressPage addressPage = new AddressPage(cart);
        mainPanel.add(addressPage, "AddressPage");
        cardLayout.show(mainPanel, "AddressPage");
    }

    public static void showPharmacyPaymentPage(Map<Medicine, Integer> cart, String address) {
        PharmacyPaymentPage paymentPage = new PharmacyPaymentPage(cart, address);
        mainPanel.add(paymentPage, "PharmacyPaymentPage");
        cardLayout.show(mainPanel, "PharmacyPaymentPage");
    }

    public static void showPharmacyEnhancedPaymentPage(Map<Medicine, Integer> cart, String address, double totalAmount) {
        EnhancedOnlinePaymentPage paymentPage = new EnhancedOnlinePaymentPage(cart, address, totalAmount, true);
        mainPanel.add(paymentPage, "PharmacyEnhancedPaymentPage");
        cardLayout.show(mainPanel, "PharmacyEnhancedPaymentPage");
    }

    public static class TempPatientData {
        public String name;
        public String contactNumber;
        public int age;
        public String symptoms;

        public TempPatientData(String name, String contactNumber, int age) {
            this.name = name;
            this.contactNumber = contactNumber;
            this.age = age;
        }
    }
    public static void showDoctorSpecificPage(String panelName, Doctor doctor) {
        JPanel newPage = null;
        if (panelName.equals("AppointmentsViewPageDoctor")) {
            newPage = new AppointmentsViewPage(false, doctor);
        } else if (panelName.equals("PatientDetailsPageDoctor")) {
            newPage = new PatientDetailsPage(false, doctor);
        }
        if (newPage != null) {
            mainPanel.add(newPage, panelName);
            cardLayout.show(mainPanel, panelName);
        }
    }

    // --- Navigation methods for the multi-step appointment booking process ---

    public static void startAppointmentBooking(Patient patient) {
        AppointmentBookingPage2 page2 = new AppointmentBookingPage2(patient);
        mainPanel.add(page2, "AppointmentBookingPage2");
        cardLayout.show(mainPanel, "AppointmentBookingPage2");
    }

    public static void showDoctorSuggestions(Patient patient, String symptoms) {
        AppointmentBookingPage25 page25 = new AppointmentBookingPage25(patient, symptoms);
        mainPanel.add(page25, "AppointmentBookingPage25");
        cardLayout.show(mainPanel, "AppointmentBookingPage25");
    }

    public static void startAppointmentBookingWithTempData() {
        AppointmentBookingPage2 page2 = new AppointmentBookingPage2(null); // Pass null for temp data
        mainPanel.add(page2, "AppointmentBookingPage2");
        cardLayout.show(mainPanel, "AppointmentBookingPage2");
    }

    public static void showDoctorSuggestionsWithTempData(String symptoms) {
        tempPatientData.symptoms = symptoms;
        AppointmentBookingPage25 page25 = new AppointmentBookingPage25(null, symptoms);
        mainPanel.add(page25, "AppointmentBookingPage25");
        cardLayout.show(mainPanel, "AppointmentBookingPage25");
    }

    public static void showDoctorCalendar(Patient patient, Doctor doctor, String reason) {
        AppointmentBookingPage3 page3 = new AppointmentBookingPage3(patient, doctor, reason);
        mainPanel.add(page3, "AppointmentBookingPage3");
        cardLayout.show(mainPanel, "AppointmentBookingPage3");
    }

    public static void showDoctorCalendarWithTempData(Doctor doctor, String reason) {
        // Create a temporary patient for calendar navigation
        Patient tempPatient = new Patient(0,
                tempPatientData.name,
                tempPatientData.contactNumber,
                tempPatientData.age);
        AppointmentBookingPage3 page3 = new AppointmentBookingPage3(tempPatient, doctor, reason);
        mainPanel.add(page3, "AppointmentBookingPage3");
        cardLayout.show(mainPanel, "AppointmentBookingPage3");
    }

    public static void showTimeSlots(Patient patient, Doctor doctor, String reason, LocalDate date) {
        AppointmentBookingPage4 page4 = new AppointmentBookingPage4(patient, doctor, reason, date);
        mainPanel.add(page4, "AppointmentBookingPage4");
        cardLayout.show(mainPanel, "AppointmentBookingPage4");
    }

    public static void showPatientInfoPage(Patient patient) {
        PatientInfoPage page = new PatientInfoPage(patient);
        mainPanel.add(page, "PatientInfoPage");
        cardLayout.show(mainPanel, "PatientInfoPage");
    }
    private static boolean isConnectedToHospitalNetwork() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            String ipAddress = address.getHostAddress();

            // Check if it's a doctor IP
            if (isHospitalIP(ipAddress)) {
                return true;
            }
            return false; // Public IP
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHospitalIP(String ipAddress) {
        // Hospital IP addresses (same for both admin and doctor)
        String[] hospitalIPs = {
                "192.168.1.7",        // Hospital network IP 1
                "10.3.1.64",     // Hospital network IP 2
                "127.0.0.1",          // localhost
                "192.168.1.0",
                // Add more hospital IPs as needed
        };

        for (String hospitalIP : hospitalIPs) {
            if (ipAddress.equals(hospitalIP) ||
                    ipAddress.startsWith(hospitalIP.substring(0, hospitalIP.lastIndexOf(".")) + ".")) {
                return true;
            }
        }
        return false;
    }


    public static String getMacAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);

            if (ni != null) {
                byte[] hardwareAddress = ni.getHardwareAddress();
                if (hardwareAddress != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        sb.append(String.format("%02X%s", hardwareAddress[i],
                                (i < hardwareAddress.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAdminMacAddress(String macAddress) {
        // Define admin MAC addresses
        String[] adminMacAddresses = {
                "94-BB-43-03-67-D1",        // Admin workstation 1
                "11-22-33-44-55-66"         // Admin workstation 2
                // Add more admin MAC addresses as needed
        };

        if (macAddress != null) {
            for (String adminMac : adminMacAddresses) {
                if (macAddress.equalsIgnoreCase(adminMac)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static void printNetworkInfo() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Host IP: " + localHost.getHostAddress());

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i],
                                    (i < mac.length - 1) ? "-" : ""));
                        }
                        System.out.println("Interface: " + ni.getName() +
                                ", MAC: " + sb.toString() +
                                ", Display Name: " + ni.getDisplayName());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Add auto-routing method
    public static void autoRouteBasedOnIP() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            String ipAddress = address.getHostAddress();
            String macAddress = getMacAddress();

            System.out.println("DEBUG - IP: " + ipAddress + ", MAC: " + macAddress);

            // Check if connected to hospital network
            if (isHospitalIP(ipAddress)) {
                // Same IP for hospital network - differentiate using MAC address
                if (isAdminMacAddress(macAddress)) {
                    // Admin MAC detected - Go to MainPage
                    showPage("MainPage");
                    showNotification("Administrator workstation detected", COLOR_SUCCESS);
                } else {
                    // Hospital staff (doctor/nurse) - Go to HospitalMainPage
                    showPage("HospitalMainPage");
                    showNotification("Hospital staff workstation detected", new Color(25, 118, 210));
                }
            } else {
                // Not hospital IP - Show public interface
                showPage("PublicMainPage");
            }
        } catch (Exception e) {
            // Default to public on error
            showPage("PublicMainPage");
            System.err.println("Error in auto-routing: " + e.getMessage());
        }
    }
    public static void showNotification(String message, Color backgroundColor) {
        // Create a temporary notification panel
        JPanel notificationPanel = new JPanel();
        notificationPanel.setBackground(backgroundColor);
        notificationPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        notificationPanel.add(messageLabel);

        // Show as a temporary overlay or print to console for now
        System.out.println("NOTIFICATION: " + message);
    }
    // --- Getters and Setters for session management ---
    public static void setCurrentAdminUser(String username) {
        currentAdminUser = username;
    }

    public static void setCurrentDoctorUser(Doctor doctor) {
        currentDoctorUser = doctor;
    }

    public static DatabaseManager getDbManager() {
        return dbManager;
    }

    // =================================================================================
    // All classes below are now static inner classes of HospitalManagementSystem
    // =================================================================================

    // Data Models
    public static class Doctor {
        private String id, name, specialization, username;
        private boolean isOnDuty;

        public Doctor(String id, String name, String specialization, String username, boolean isOnDuty) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
            this.username = username;
            this.isOnDuty = isOnDuty;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSpecialization() {
            return specialization;
        }

        public String getUsername() {
            return username;
        }

        public boolean isOnDuty() {
            return isOnDuty;
        }
    }

    public static class Patient {
        private int id;
        private String name, contactNumber;
        private int age;

        public Patient(int id, String name, String contactNumber) {
            this.id = id;
            this.name = name;
            this.contactNumber = contactNumber;
            this.age = 0; // Default age
        }

        public Patient(int id, String name, String contactNumber, int age) {
            this.id = id;
            this.name = name;
            this.contactNumber = contactNumber;
            this.age = age;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getContactNumber() {
            return contactNumber;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class MainPage extends JPanel {
        private JLabel scrollingLabel;
        private Timer scrollTimer;
        private int scrollPosition = 0;

        public MainPage() {
            setOpaque(false);
            setLayout(new BorderLayout());
            // --- Top Navigation Panel ---
            JPanel navigationPanel = new JPanel(new BorderLayout());
            navigationPanel.setOpaque(false);
            navigationPanel.setBorder(new EmptyBorder(15, 40, 15, 40));
            // Hospital Logo and Name
            JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            logoPanel.setOpaque(false);
            JLabel logoIcon = new JLabel("🏥");
            logoIcon.setFont(new Font("SansSerif", Font.BOLD, 32));
            logoIcon.setForeground(Color.WHITE);
            JLabel hospitalName = new JLabel("SALVE Memorial Hospital");
            hospitalName.setFont(new Font("Serif", Font.BOLD, 28));
            hospitalName.setForeground(Color.WHITE);
            logoPanel.add(logoIcon);
            logoPanel.add(hospitalName);

            // Navigation Menu
            JPanel navMenu = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
            navMenu.setOpaque(false);
            // Login Button
            JButton loginButton = new JButton("Login");
            stylePrimaryButton(loginButton);
            loginButton.addActionListener(e -> HospitalManagementSystem.showPage("LoginPage"));
            navigationPanel.add(logoPanel, BorderLayout.WEST);
            navigationPanel.add(navMenu, BorderLayout.CENTER);
            navigationPanel.add(loginButton, BorderLayout.EAST);
            add(navigationPanel, BorderLayout.NORTH);

            // --- Main Content Panel ---
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setOpaque(false);
            contentPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

            // Hero Section
            JPanel heroSection = new JPanel(new BorderLayout());
            heroSection.setOpaque(false);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            JLabel mainTitle = new JLabel("Your Health Is");
            mainTitle.setFont(new Font("", Font.BOLD, 48));
            mainTitle.setForeground(Color.WHITE);
            mainTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel subTitle = new JLabel("Our Priority.");
            subTitle.setFont(new Font("SansSerif", Font.BOLD, 48));
            subTitle.setForeground(new Color(100, 149, 237));
            // Cornflower blue
            subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel description = new JLabel("<html>Comprehensive healthcare services with experienced doctors<br>" +
                    "and state-of-the-art facilities. Your health is our mission<br>" +
                    "and we're committed to providing quality care.</html>");
            description.setFont(new Font("SansSerif", Font.PLAIN, 16));
            description.setForeground(new Color(220, 220, 220));
            description.setAlignmentX(Component.LEFT_ALIGNMENT);
            description.setBorder(new EmptyBorder(20, 0, 30, 0));
            JButton heroButton = new JButton("Book An Appointment");
            stylePrimaryButton(heroButton);
            heroButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            heroButton.addActionListener(e -> HospitalManagementSystem.showPage("AppointmentBookingPage1"));
            heroButton.setAlignmentX(Component.LEFT_ALIGNMENT);

            textPanel.add(mainTitle);
            textPanel.add(subTitle);
            textPanel.add(description);
            textPanel.add(heroButton);

            heroSection.add(textPanel, BorderLayout.WEST);
            contentPanel.add(heroSection, BorderLayout.CENTER);

            // --- Feature Services Panel ---
            JPanel servicesPanel = new JPanel(new GridLayout(1, 3, 30, 0));
            servicesPanel.setOpaque(false);
            servicesPanel.setBorder(new EmptyBorder(60, 0, 0, 0));

            // Service 1
            JPanel service1 = createServiceBox(
                    "👤",
                    "Patient Portal",
                    "Access your medical records and consultation history",
                    () -> HospitalManagementSystem.showPage("PatientLoginPage")

            );

            // Service 2
            JPanel service2 = createServiceBox(
                    "💳",
                    "Online Bill Pay",
                    "Secure online payment for hospital bills and services",
                    () -> HospitalManagementSystem.showPage("OnlinePaymentPage")
            );
            // Service 3
            JPanel service3 = createServiceBox(
                    "💊",
                    "Online Pharmacy",
                    "Order medicines and health products for delivery",
                    () -> HospitalManagementSystem.showPage("OnlinePharmacyPage")
            );
            servicesPanel.add(service1);
            servicesPanel.add(service2);
            servicesPanel.add(service3);

            contentPanel.add(servicesPanel, BorderLayout.SOUTH);
            add(contentPanel, BorderLayout.CENTER);

            // --- NEW: Scrolling Toll-Free Number Banner at Bottom ---
            add(createScrollingBanner(), BorderLayout.SOUTH);
        }

        private JPanel createScrollingBanner() {
            JPanel bannerPanel = new JPanel(null);
            bannerPanel.setOpaque(false);
            bannerPanel.setPreferredSize(new Dimension(0, 50));
            bannerPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

            scrollingLabel = new JLabel("🚨 24/7 Emergency Helpline: 1800-SALVE-HELP | 🆓 Free Consultation Call Now! | 🚑 Ambulance Service Available");
            scrollingLabel.setForeground(Color.WHITE);
            scrollingLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            scrollingLabel.setOpaque(false);

            Dimension labelSize = scrollingLabel.getPreferredSize();
            scrollingLabel.setBounds(0, 15, labelSize.width, 20);
            bannerPanel.add(scrollingLabel);
            startScrollingAnimation();
            return bannerPanel;
        }

        private void startScrollingAnimation() {
            scrollTimer = new Timer(50, e -> {
                if (scrollingLabel != null) {
                    Container parent = scrollingLabel.getParent();
                    if (parent != null && parent.getWidth() > 0) {
                        int parentWidth = parent.getWidth();
                        int labelWidth = scrollingLabel.getPreferredSize().width;
                        scrollPosition -= 2;
                        if (scrollPosition < -labelWidth - 20) {
                            scrollPosition = parentWidth;
                        }
                        scrollingLabel.setLocation(scrollPosition, 15);
                        parent.repaint();
                    }
                }
            });

            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    if (scrollTimer != null && !scrollTimer.isRunning()) {
                        scrollPosition = getWidth();
                        scrollTimer.start();
                    }
                }
                @Override
                public void componentHidden(java.awt.event.ComponentEvent e) {
                    if (scrollTimer != null && scrollTimer.isRunning()) {
                        scrollTimer.stop();
                    }
                }
            });
        }

        private JPanel createServiceBox(String icon, String title, String description, Runnable action) {
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBackground(new Color(255, 255, 255, 240));
            box.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(230, 230, 230), 1),
                    new EmptyBorder(25, 20, 25, 20)
            ));
            box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            final JPanel finalBox = box; // For the inner class
            box.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }

                @Override

                public void mouseEntered(MouseEvent e) {
                    finalBox.setBackground(new Color(248, 249, 250));
                    finalBox.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    finalBox.setBackground(new Color(255, 255, 255, 240));

                    finalBox.repaint();
                }
            });
            // Icon with colored background circle
            JPanel iconPanel = new JPanel();
            iconPanel.setOpaque(false);
            iconPanel.setPreferredSize(new Dimension(60, 60));
            iconPanel.setMaximumSize(new Dimension(60, 60));
            iconPanel.setLayout(new BorderLayout());

            JLabel iconLabel = new JLabel(icon);
            iconLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setVerticalAlignment(SwingConstants.CENTER);
            iconLabel.setOpaque(true);
            iconLabel.setBackground(new Color(30, 136, 229, 30));
            iconLabel.setPreferredSize(new Dimension(60, 60));
            iconPanel.add(iconLabel, BorderLayout.CENTER);
            iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            titleLabel.setForeground(new Color(51, 51, 51));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setBorder(new EmptyBorder(15, 0, 10, 0));

            JLabel descLabel = new JLabel("<html><center>" + description + "</center></html>");
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            descLabel.setForeground(new Color(120, 120, 120));
            descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            box.add(iconPanel);
            box.add(titleLabel);
            box.add(descLabel);

            return box;
        }

        private void stylePrimaryButton(JButton button) {
            button.setBackground(new Color(220, 53, 69));
            // Red color like in the image
            button.setForeground(Color.WHITE);
            button.setFont(new Font("SansSerif", Font.BOLD, 14));
            button.setFocusPainted(false);
            button.setBorder(new EmptyBorder(12, 25, 12, 25));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }
    public static class Consultation {
        private String id, doctorId, consultingReason;
        private int patientId;
        private LocalDateTime consultationDateTime;
        private LocalDate nextConsultingDate;

        public Consultation(String id, int pId, String dId, LocalDateTime dt, String reason, LocalDate nextDate) {
            this.id = id;
            this.patientId = pId;
            this.doctorId = dId;
            this.consultationDateTime = dt;
            this.consultingReason = reason;
            this.nextConsultingDate = nextDate;
        }

        public String getId() {
            return id;
        }

        public int getPatientId() {
            return patientId;
        }

        public String getDoctorId() {
            return doctorId;
        }

        public LocalDateTime getConsultationDateTime() {
            return consultationDateTime;
        }

        public String getConsultingReason() {
            return consultingReason;
        }

        public LocalDate getNextConsultingDate() {
            return nextConsultingDate;
        }
    }

    public static class Prescription {
        private String id, consultationId, medicineName, dosage;

        public Prescription(String id, String cId, String medName, String dosage) {
            this.id = id;
            this.consultationId = cId;
            this.medicineName = medName;
            this.dosage = dosage;
        }

        public String getId() { return id; }
        public String getMedicineName() { return medicineName; }
        public String getDosage() { return dosage; }
    }

    public static class Medicine {
        private int id;
        private String name;
        private double price;
        private int stock;

        public Medicine(int id, String name, double price, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public int getStock() {
            return stock;
        }
    }

    public static class MedicalTest {
        private int id;
        private String name, description, category;
        private double price;

        public MedicalTest(int id, String name, String description, double price, String category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.category = category;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getPrice() {
            return price;
        }

        public String getCategory() {
            return category;
        }
    }

    // Database Manager
    public static class DatabaseManager {
        private static final String JDBC_URL = "jdbc:mysql://salvehospital-salvehospital.d.aivencloud.com:28434/salve_memorial_hospital?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        private static final String USER = "avnadmin";
        private static final String PASSWORD = "AVNS_ifCRh-mpXcyrldibIv0";

        public DatabaseManager() {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            try {
                this.createDatabaseIfNotExists();
                this.createTables();
                this.addSampleData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public Connection getConnection() throws SQLException {
            try {
                return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
            } catch (SQLException e) {
                System.err.println("Connection failed to " + JDBC_URL);
                throw e;
            }
        }

        private void createDatabaseIfNotExists() throws SQLException {
            String createDbUrl = "jdbc:mysql://salvehospital-salvehospital.d.aivencloud.com:28434?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            try (Connection conn = DriverManager.getConnection(createDbUrl, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS salve_memorial_hospital");
            }
        }

        private void createTables() throws SQLException {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                // Users table
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "username VARCHAR(255) PRIMARY KEY, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "role VARCHAR(50) NOT NULL)");

                // Doctors table - using 'isonduty' (no underscores)
                stmt.execute("CREATE TABLE IF NOT EXISTS doctors (" +
                        "id VARCHAR(255) PRIMARY KEY, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "specialization VARCHAR(255) NOT NULL, " +
                        "username VARCHAR(255) UNIQUE NOT NULL, " +
                        "is_on_duty BOOLEAN DEFAULT FALSE, " +
                        "FOREIGN KEY (username) REFERENCES users(username))");

                // Patients table
                stmt.execute("CREATE TABLE IF NOT EXISTS patients (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "contact_number VARCHAR(20), " +
                        "age INT NOT NULL)");

                // Consultations table
                stmt.execute("CREATE TABLE IF NOT EXISTS consultations (" +
                        "id VARCHAR(255) PRIMARY KEY, " +
                        "patient_id INT NOT NULL, " +
                        "doctor_id VARCHAR(255) NOT NULL, " +
                        "consultation_datetime TIMESTAMP NOT NULL, " +
                        "consulting_reason TEXT NOT NULL, " +
                        "next_consulting_date DATE, " +
                        "FOREIGN KEY (patient_id) REFERENCES patients(id), " +
                        "FOREIGN KEY (doctor_id) REFERENCES doctors(id))");

                // Prescriptions table
                stmt.execute("CREATE TABLE IF NOT EXISTS prescriptions (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "consultation_id VARCHAR(255) NOT NULL, " +
                        "medicine_name VARCHAR(255) NOT NULL, " +
                        "dosage VARCHAR(255), " +
                        "FOREIGN KEY (consultation_id) REFERENCES consultations(id))");

                // Doctor log status table
                stmt.execute("CREATE TABLE IF NOT EXISTS doctor_log_status (" +
                        "id VARCHAR(255) PRIMARY KEY, " +
                        "doctor_id VARCHAR(255) NOT NULL, " +
                        "login_time TIMESTAMP NOT NULL, " +
                        "logout_time TIMESTAMP NULL DEFAULT NULL, " +
                        "FOREIGN KEY (doctor_id) REFERENCES doctors(id))");

                // Doctor availability table - WITH DATE (no dayofweek column)
                stmt.execute("CREATE TABLE IF NOT EXISTS doctor_availability (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "doctor_id VARCHAR(255) NOT NULL, " +
                        "availability_date DATE NOT NULL, " +
                        "start_time TIME NOT NULL, " +
                        "end_time TIME NOT NULL, " +
                        "UNIQUE KEY unique_doctor_date_time (doctor_id, availability_date, start_time), " +
                        "FOREIGN KEY (doctor_id) REFERENCES doctors(id))");

                // Medicines table
                stmt.execute("CREATE TABLE IF NOT EXISTS medicines (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "price DOUBLE NOT NULL, " +
                        "stock INT NOT NULL)");

                // Medical tests table - 'medicaltests' (no underscore)
                stmt.execute("CREATE TABLE IF NOT EXISTS medical_tests (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "description TEXT, " +
                        "price DOUBLE NOT NULL, " +
                        "category VARCHAR(100))");

                // Consultation tests table
                stmt.execute("CREATE TABLE IF NOT EXISTS consultation_tests (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "consultation_id VARCHAR(255) NOT NULL, " +
                        "test_id INT NOT NULL, " +
                        "quantity INT DEFAULT 1, " +
                        "FOREIGN KEY (consultation_id) REFERENCES consultations(id), " +
                        "FOREIGN KEY (test_id) REFERENCES medical_tests(id))");

                System.out.println("✓ All tables created successfully");

                // Check and add age column if it doesn't exist (for backward compatibility)
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet rs = metaData.getColumns(null, null, "patients", "age");
                if (!rs.next()) {
                    System.out.println("Updating patients table: Adding age column...");
                    stmt.executeUpdate("ALTER TABLE patients ADD COLUMN age INT NOT NULL DEFAULT 0");
                    System.out.println("✓ age column added successfully.");
                }
                rs.close();

            } catch (SQLException e) {
                System.err.println("✗ Error creating tables: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }

        private void addSampleData() {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            addUser("admin", "admin123", "Admin");
            addUser("asmith", "doc123", "Doctor");
            addUser("bjohnson", "doc123", "Doctor");
            addUser("cjones", "doc123", "Doctor");

            // USE FIXED UUIDs INSTEAD OF RANDOM ONES - THIS IS THE KEY FIX
            Doctor doc1 = new Doctor("8d6be7ee-65e2-48ad-9dc1-55bf48d276e8", "Alice Smith", "Cardiology", "asmith", false);
            Doctor doc2 = new Doctor("a3bdb5c8-074c-44a8-aa89-cfc56494e8b7", "Bob Johnson", "Pediatrics", "bjohnson", false);
            Doctor doc3 = new Doctor("06c51dc1-c7f5-48da-9859-39c9645c8ead", "Carol Jones", "General Medicine", "cjones", false);

            addDoctor(doc1);
            addDoctor(doc2);
            addDoctor(doc3);

            addMedicine("Paracetamol", 20.00, 100);
            addMedicine("Aspirin", 35.50, 50);
            addMedicine("Ibuprofen", 45.00, 75);
            addSampleTestData();
        }

        private void addSampleTestData() {
            addMedicalTest("Blood Test - Complete Blood Count (CBC)", "Comprehensive blood analysis including RBC, WBC, platelets", 450.00, "Blood Tests");
            addMedicalTest("X-Ray Chest", "Chest X-ray to examine lungs and heart", 350.00, "Radiology");
            addMedicalTest("ECG (Electrocardiogram)", "Heart rhythm and electrical activity test", 300.00, "Cardiology");
            addMedicalTest("Urine Analysis", "Complete urine examination", 200.00, "Pathology");
            addMedicalTest("Blood Sugar (Fasting)", "Fasting blood glucose level test", 150.00, "Blood Tests");
            addMedicalTest("Lipid Profile", "Cholesterol and triglycerides analysis", 500.00, "Blood Tests");
            addMedicalTest("Ultrasound Abdomen", "Abdominal organ imaging", 800.00, "Radiology");
            addMedicalTest("Thyroid Function Test", "TSH, T3, T4 hormone levels", 600.00, "Endocrinology");
        }

        public void addMedicalTest(String name, String description, double price, String category) {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO medical_tests (name, description, price, category) VALUES (?, ?, ?, ?)")) {
                pstmt.setString(1, name);
                pstmt.setString(2, description);
                pstmt.setDouble(3, price);
                pstmt.setString(4, category);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void addUser(String username, String password, String role) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT IGNORE INTO users (username, password, role) VALUES (?, ?, ?)")) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, role);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void addDoctor(Doctor doctor) {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT IGNORE INTO doctors (id, name, specialization, username, is_on_duty) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, doctor.getId());
                pstmt.setString(2, doctor.getName());
                pstmt.setString(3, doctor.getSpecialization());
                pstmt.setString(4, doctor.getUsername());
                pstmt.setBoolean(5, false);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void addMedicine(String name, double price, int stock) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT IGNORE INTO medicines (name, price, stock) VALUES (?, ?, ?)")) {
                pstmt.setString(1, name);
                pstmt.setDouble(2, price);
                pstmt.setInt(3, stock);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public int addPatient(Patient patient) {
            String sql = "INSERT INTO patients (name, contact_number, age) VALUES (?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, patient.getName());
                pstmt.setString(2, patient.getContactNumber());
                pstmt.setInt(3, patient.getAge());
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }

        public void recordConsultation(int pId, String dId, String reason, LocalDateTime dateTime, LocalDate nextDate) {
            try {
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO consultations (id, patient_id, doctor_id, consultation_datetime, consulting_reason, next_consulting_date) VALUES (?, ?, ?, ?, ?, ?)"
                );
                pstmt.setString(1, UUID.randomUUID().toString());
                pstmt.setInt(2, pId);
                pstmt.setString(3, dId);
                pstmt.setTimestamp(4, Timestamp.valueOf(dateTime)); // Use the provided dateTime
                pstmt.setString(5, reason);
                pstmt.setDate(6, nextDate != null ? Date.valueOf(nextDate) : null);
                pstmt.executeUpdate();
                System.out.println("Consultation recorded with date: " + dateTime);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        public List<Doctor> getAllDoctors() {
            List<Doctor> doctors = new ArrayList<>();
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM doctors")) {
                while (rs.next()) {
                    doctors.add(new Doctor(rs.getString("id"), rs.getString("name"), rs.getString("specialization"), rs.getString("username"), rs.getBoolean("is_on_duty")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return doctors;
        }

        public List<Doctor> getDoctorsOnDuty() {
            List<Doctor> onDutyDoctors = new ArrayList<>();
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM doctors WHERE is_on_duty = TRUE")) {
                while (rs.next()) {
                    onDutyDoctors.add(new Doctor(rs.getString("id"), rs.getString("name"), rs.getString("specialization"), rs.getString("username"), rs.getBoolean("is_on_duty")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return onDutyDoctors;
        }

        public List<Doctor> getSuggestedDoctorsMultiple(String symptoms, int patientAge) {
            List<Doctor> suggestedDoctors = new ArrayList<>();
            Set<String> specializations = new HashSet<>();

            // Step 1: Add age-based specialization
            if (patientAge < 18) {
                specializations.add("Pediatrics"); // Always include pediatrics for children
            }

            // Step 2: Add symptom-based specializations
            List<String> symptomSpecializations = getSpecializationsFromSymptoms(symptoms);
            specializations.addAll(symptomSpecializations);

            // Step 3: Get doctors from ALL relevant specializations
            for (String specialization : specializations) {
                List<Doctor> doctorsForSpecialization = getDoctorsBySpecialization(specialization);
                suggestedDoctors.addAll(doctorsForSpecialization);
            }

            // Step 4: If no specific doctors found, add General Medicine as fallback
            if (suggestedDoctors.isEmpty()) {
                List<Doctor> generalDoctors = getDoctorsBySpecialization("General Medicine");
                suggestedDoctors.addAll(generalDoctors);
            }

            return suggestedDoctors;
        }
        // Helper method to get all specializations based on symptoms
        private List<String> getSpecializationsFromSymptoms(String symptoms) {
            List<String> specializations = new ArrayList<>();
            symptoms = symptoms.toLowerCase();

            // Heart/Chest related - add Cardiology
            if (symptoms.contains("heart") || symptoms.contains("chest") || symptoms.contains("cardiac") ||
                    symptoms.contains("palpitation") || symptoms.contains("angina") || symptoms.contains("arrhythmia")) {
                specializations.add("Cardiology");
            }

            // Bone/Joint related - add Orthopedics
            if (symptoms.contains("bone") || symptoms.contains("joint") || symptoms.contains("fracture") ||
                    symptoms.contains("muscle") || symptoms.contains("spine") || symptoms.contains("arthritis")) {
                specializations.add("Orthopedics");
            }

            // Skin related - add Dermatology
            if (symptoms.contains("skin") || symptoms.contains("rash") || symptoms.contains("allergy") ||
                    symptoms.contains("eczema") || symptoms.contains("acne")) {
                specializations.add("Dermatology");
            }

            // Eye related - add Ophthalmology
            if (symptoms.contains("eye") || symptoms.contains("vision") || symptoms.contains("blind") ||
                    symptoms.contains("glasses") || symptoms.contains("cataract")) {
                specializations.add("Ophthalmology");
            }

            // Child-specific symptoms
            if (symptoms.contains("child") || symptoms.contains("baby") || symptoms.contains("infant")) {
                specializations.add("Pediatrics");
            }

            return specializations;
        }

        // Helper method to get doctors by specialization
        // Replace the problematic method in your DatabaseManager class
        private List<Doctor> getDoctorsBySpecialization(String specialization) {
            List<Doctor> doctors = new ArrayList<>();
            String sql = "SELECT * FROM doctors WHERE specialization = ?";

            try (Connection conn = getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, specialization);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    // Check if isonduty column exists, default to false if not
                    boolean isOnDuty = false;
                    try {
                        isOnDuty = rs.getBoolean("is_on_duty");
                    } catch (SQLException e) {
                        // Column doesn't exist, use default value
                        isOnDuty = false;
                    }

                    doctors.add(new Doctor(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("specialization"),
                            rs.getString("username"),
                            isOnDuty
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return doctors;
        }
        public List<Patient> getAllPatients() {
            List<Patient> patients = new ArrayList<>();
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM patients")) {
                while (rs.next()) {
                    patients.add(new Patient(rs.getInt("id"), rs.getString("name"), rs.getString("contact_number"), rs.getInt("age")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return patients;
        }

        public List<LocalDate> getDoctorAvailableDates(String doctorId) {
            List<LocalDate> dates = new ArrayList<>();
            try {
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT DISTINCT availability_date FROM doctor_availability " +
                                "WHERE doctor_id = ? AND availability_date >= CURDATE() " +
                                "ORDER BY availability_date"
                );
                pstmt.setString(1, doctorId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    dates.add(rs.getDate("availability_date").toLocalDate());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return dates;
        }

        public List<LocalTime> getAvailableTimeSlotsForDate(String doctorId, LocalDate date) {
            List<LocalTime> slots = new ArrayList<>();
            try {
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT start_time, end_time FROM doctor_availability " +
                                "WHERE doctor_id = ? AND availability_date = ?"
                );
                pstmt.setString(1, doctorId);
                pstmt.setDate(2, java.sql.Date.valueOf(date));
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    LocalTime start = rs.getTime("start_time").toLocalTime();
                    LocalTime end = rs.getTime("end_time").toLocalTime();

                    // Generate 30-minute slots
                    while (start.isBefore(end)) {
                        slots.add(start);
                        start = start.plusMinutes(30);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return slots;
        }

        public List<Medicine> searchMedicines(String query) {
            List<Medicine> medicines = new ArrayList<>();
            String sql = "SELECT * FROM medicines WHERE LOWER(name) LIKE LOWER(?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + query + "%");
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    medicines.add(new Medicine(rs.getInt("id"), rs.getString("name"), rs.getDouble("price"), rs.getInt("stock")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return medicines;
        }

        public void recordDoctorLogin(String doctorId) {
            recordDoctorLogout(doctorId);
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO doctor_log_status (id, doctor_id, login_time) VALUES (?, ?, ?)")) {
                pstmt.setString(1, UUID.randomUUID().toString());
                pstmt.setString(2, doctorId);
                pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void recordDoctorLogout(String doctorId) {
            try {
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE doctor_log_status SET logout_time = ? WHERE doctor_id = ? AND logout_time IS NULL"
                );
                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(2, doctorId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public void importDoctorScheduleFromCSV(String filePath) throws Exception {
            String sql = "INSERT INTO doctor_availability (doctor_id, availability_date, start_time, end_time) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time)";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM doctors WHERE name = ?");
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(filePath))) {

                String line = reader.readLine(); // Skip header line
                int batchCount = 0;
                int skippedCount = 0;
                StringBuilder skippedDoctors = new StringBuilder();
                java.time.format.DateTimeFormatter dateFormatter =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");

                    if (data.length >= 4) {
                        String doctorName = data[0].trim();

                        // Look up doctor ID by name
                        checkStmt.setString(1, doctorName);
                        ResultSet rs = checkStmt.executeQuery();

                        if (!rs.next()) {
                            skippedDoctors.append("\n - ").append(doctorName);
                            skippedCount++;
                            rs.close();
                            continue;
                        }

                        String doctorId = rs.getString("id");
                        rs.close();

                        // Set parameters for insert
                        stmt.setString(1, doctorId); // doctorid

                        // Parse DD/MM/YYYY format to SQL DATE
                        LocalDate date = LocalDate.parse(data[1].trim(), dateFormatter);
                        stmt.setDate(2, java.sql.Date.valueOf(date)); // availabilitydate

                        stmt.setTime(3, Time.valueOf(data[2].trim())); // starttime
                        stmt.setTime(4, Time.valueOf(data[3].trim())); // endtime

                        stmt.addBatch();
                        batchCount++;

                        // Execute batch every 100 records
                        if (batchCount % 100 == 0) {
                            stmt.executeBatch();
                        }
                    }
                }

                // Execute remaining records
                if (batchCount % 100 != 0 && batchCount > 0) {
                    stmt.executeBatch();
                }

                String message = "Successfully imported " + batchCount + " schedule records.";
                if (skippedCount > 0) {
                    message += "\n" + skippedCount + " records with invalid doctor names:" + skippedDoctors.toString();
                }

                if (batchCount == 0 && skippedCount > 0) {
                    throw new Exception("No records imported! All doctor names were invalid.");
                }

                System.out.println(message);

            } catch (SQLException e) {
                throw new Exception("Database error during CSV import: " + e.getMessage(), e);
            } catch (java.io.IOException e) {
                throw new Exception("File reading error: " + e.getMessage(), e);
            } catch (java.time.format.DateTimeParseException e) {
                throw new Exception("Date format error. Please use DD/MM/YYYY format: " + e.getMessage(), e);
            }
        }
        public Medicine getMedicineByName(String medicineName) {
            try {
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT * FROM medicines WHERE name = ?"
                );
                pstmt.setString(1, medicineName);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return new Medicine(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("stock")
                    );
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        public void updateDoctorDutyStatus(String doctorId, boolean isOnDuty) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("UPDATE doctors SET is_on_duty = ? WHERE id = ?")) {
                pstmt.setBoolean(1, isOnDuty);
                pstmt.setString(2, doctorId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public boolean isDoctorOnDuty(String doctorId) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT is_on_duty FROM doctors WHERE id = ?")) {
                pstmt.setString(1, doctorId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) return rs.getBoolean("is_on_duty");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        public String[] getDoctorCurrentLoginStatus(String doctorId) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT login_time, logout_time FROM doctor_log_status WHERE doctor_id = ? ORDER BY login_time DESC LIMIT 1")) {
                pstmt.setString(1, doctorId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Timestamp logoutTime = rs.getTimestamp("logout_time");
                    Timestamp loginTime = rs.getTimestamp("login_time");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    if (logoutTime == null) {
                        return new String[]{"🟢 Logged In", loginTime.toLocalDateTime().format(formatter)};
                    } else {
                        return new String[]{"🔴 Logged Out", logoutTime.toLocalDateTime().format(formatter)};
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new String[]{"❓ Unknown", "N/A"};
        }

        public Patient getPatientById(int patientId) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM patients WHERE id = ?")) {
                pstmt.setInt(1, patientId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next())
                    return new Patient(rs.getInt("id"), rs.getString("name"), rs.getString("contact_number"), rs.getInt("age"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        public List<Prescription> getAllPrescriptionsForPatient(int patientId) {
            List<Prescription> allPrescriptions = new ArrayList<>();
            try {
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT p.* FROM prescriptions p " +
                                "JOIN consultations c ON p.consultation_id = c.id " +  // Fixed: consultation_id
                                "WHERE c.patient_id = ? " +  // Fixed: patient_id
                                "ORDER BY c.consultation_datetime DESC"  // Fixed: consultation_datetime
                );
                pstmt.setInt(1, patientId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    allPrescriptions.add(new Prescription(
                            rs.getString("id"),
                            rs.getString("consultation_id"),  // Fixed: consultation_id
                            rs.getString("medicine_name"),    // Fixed: medicine_name
                            rs.getString("dosage")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return allPrescriptions;
        }

        public Doctor getDoctorById(String doctorId) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM doctors WHERE id = ?")) {
                pstmt.setString(1, doctorId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return new Doctor(rs.getString("id"), rs.getString("name"), rs.getString("specialization"), rs.getString("username"), rs.getBoolean("is_on_duty"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public List<Consultation> getAllUpcomingAppointments() {
            List<Consultation> appointments = new ArrayList<>();
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM consultations WHERE consultation_datetime >= ? ORDER BY consultation_datetime ASC")) {
                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    appointments.add(new Consultation(rs.getString("id"), rs.getInt("patient_id"), rs.getString("doctor_id"), rs.getTimestamp("consultation_datetime").toLocalDateTime(), rs.getString("consulting_reason"), rs.getDate("next_consulting_date") != null ? rs.getDate("next_consulting_date").toLocalDate() : null));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return appointments;
        }

        public List<MedicalTest> getAllMedicalTests() {
            List<MedicalTest> tests = new ArrayList<>();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM medical_tests ORDER BY category, name")) {
                while (rs.next()) {
                    tests.add(new MedicalTest(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("price"),
                            rs.getString("category")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return tests;
        }

        public void addConsultationTest(String consultationId, int testId, int quantity) {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO consultation_tests (consultation_id, test_id, quantity) VALUES (?, ?, ?)")) {
                pstmt.setString(1, consultationId);
                pstmt.setInt(2, testId);
                pstmt.setInt(3, quantity);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public List<MedicalTest> getTestsForConsultation(String consultationId) {
            List<MedicalTest> tests = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT mt.* FROM medical_tests mt JOIN consultation_tests ct ON mt.id = ct.test_id WHERE ct.consultation_id = ?")) {
                pstmt.setString(1, consultationId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    tests.add(new MedicalTest(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("price"),
                            rs.getString("category")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return tests;
        }

        public List<Consultation> getUpcomingAppointments(String doctorId) {
            List<Consultation> appointments = new ArrayList<>();
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM consultations WHERE doctor_id = ? AND consultation_datetime >= ? ORDER BY consultation_datetime ASC")) {
                pstmt.setString(1, doctorId);
                pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    appointments.add(new Consultation(rs.getString("id"), rs.getInt("patient_id"), rs.getString("doctor_id"), rs.getTimestamp("consultation_datetime").toLocalDateTime(), rs.getString("consulting_reason"), rs.getDate("next_consulting_date") != null ? rs.getDate("next_consulting_date").toLocalDate() : null));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return appointments;
        }

        public List<Patient> getPatientsByName(String name) {
            List<Patient> patients = new ArrayList<>();
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM patients WHERE LOWER(name) LIKE LOWER(?)")) {
                pstmt.setString(1, "%" + name + "%");
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    patients.add(new Patient(rs.getInt("id"), rs.getString("name"), rs.getString("contact_number"), rs.getInt("age")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return patients;
        }

        public List<Patient> getPatientsByDoctor(String doctorId) {
            List<Patient> patients = new ArrayList<>();
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT p.* FROM patients p JOIN consultations c ON p.id = c.patient_id WHERE c.doctor_id = ?")) {
                pstmt.setString(1, doctorId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    patients.add(new Patient(rs.getInt("id"), rs.getString("name"), rs.getString("contact_number"), rs.getInt("age")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return patients;
        }

        public String validateUser(String username, String password) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT role FROM users WHERE username = ? AND password = ?")) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) return rs.getString("role");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Doctor getDoctorByUsername(String username) {
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM doctors WHERE username = ?")) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return new Doctor(rs.getString("id"), rs.getString("name"), rs.getString("specialization"), rs.getString("username"), rs.getBoolean("is_on_duty"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Patient getPatientByNameAndId(String name, int id) {
            try (Connection conn = getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT * FROM patients WHERE id = ? AND LOWER(name) = LOWER(?)");
                pstmt.setInt(1, id);
                pstmt.setString(2, name);

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return new Patient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("contact_number"),
                            rs.getInt("age")  // Make sure this is included
                    );
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public List<Consultation> getConsultationsForPatient(int patientId) {
            List<Consultation> consultations = new ArrayList<>();
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM consultations WHERE patient_id = ? ORDER BY consultation_datetime DESC")) {
                pstmt.setInt(1, patientId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    consultations.add(new Consultation(rs.getString("id"), rs.getInt("patient_id"), rs.getString("doctor_id"), rs.getTimestamp("consultation_datetime").toLocalDateTime(), rs.getString("consulting_reason"), rs.getDate("next_consulting_date") != null ? rs.getDate("next_consulting_date").toLocalDate() : null));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return consultations;
        }

        public List<Prescription> getPrescriptionsForConsultation(String consultationId) {
            List<Prescription> prescriptions = new ArrayList<>();
            try {
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT * FROM prescriptions WHERE consultation_id = ?"
                );
                pstmt.setString(1, consultationId);
                ResultSet rs = pstmt.executeQuery();

                // FIXED: Added the while loop to actually read the results
                while (rs.next()) {
                    prescriptions.add(new Prescription(
                            rs.getString("id"),
                            rs.getString("consultation_id"),
                            rs.getString("medicine_name"),
                            rs.getString("dosage")
                    ));
                }
            } catch (SQLException e) {
                System.err.println("Error getting prescriptions: " + e.getMessage());
                e.printStackTrace();
            }
            return prescriptions;
        }

        public void addPrescription(String consultationId, String medicineName, String dosage) {
            String sql = "INSERT INTO prescriptions (consultation_id, medicine_name, dosage) VALUES (?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, consultationId);
                pstmt.setString(2, medicineName);
                pstmt.setString(3, dosage);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            addOrUpdateMedicineToInventory(medicineName);
        }

        private void addOrUpdateMedicineToInventory(String medicineName) {
            String checkSql = "SELECT COUNT(*) FROM medicines WHERE LOWER(name) = LOWER(?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setString(1, medicineName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    Random random = new Random();
                    double price = 10.0 + (90.0 * random.nextDouble());
                    int stock = 50 + random.nextInt(151);
                    addMedicine(medicineName, price, stock);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // UI Classes
    public static class BackgroundPanel extends JPanel {
        private final BufferedImage backgroundImage;

        public BackgroundPanel(BufferedImage backgroundImage) {
            this.backgroundImage = backgroundImage;
            setLayout(new BorderLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }

    public static class PublicMainPage extends JPanel {
        private JLabel scrollingLabel;
        private Timer scrollTimer;
        private int scrollPosition = 0;

        public PublicMainPage() {
            setOpaque(false);
            setLayout(new BorderLayout());

            // --- Top Navigation Panel ---
            JPanel navigationPanel = new JPanel(new BorderLayout());
            navigationPanel.setOpaque(false);
            navigationPanel.setBorder(new EmptyBorder(15, 40, 15, 40));

            JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            logoPanel.setOpaque(false);
            JLabel logoIcon = new JLabel("🏥");
            logoIcon.setFont(new Font("SansSerif", Font.BOLD, 32));
            logoIcon.setForeground(Color.WHITE);
            JLabel hospitalName = new JLabel("SALVE Memorial Hospital");
            hospitalName.setFont(new Font("Serif", Font.BOLD, 28));
            hospitalName.setForeground(Color.WHITE);
            logoPanel.add(logoIcon);
            logoPanel.add(hospitalName);

            navigationPanel.add(logoPanel, BorderLayout.WEST);

            // CONDITIONAL: Only show Login button if NOT admin IP
            try {
                InetAddress address = InetAddress.getLocalHost();
                String ipAddress = address.getHostAddress();

                if (!isHospitalIP(ipAddress)) {
                    // CHANGED: Login button now goes to PatientLoginPage (not Staff Portal)
                    JButton loginButton = new JButton("Patient Login");
                    stylePrimaryButton(loginButton);
                    loginButton.addActionListener(e -> HospitalManagementSystem.showPage("PatientLoginPage"));
                    navigationPanel.add(loginButton, BorderLayout.EAST);
                }

            } catch (Exception e) {
                // If error detecting IP, show login button as fallback
                JButton loginButton = new JButton("Patient Login");
                stylePrimaryButton(loginButton);
                loginButton.addActionListener(e2 -> HospitalManagementSystem.showPage("PatientLoginPage"));
                navigationPanel.add(loginButton, BorderLayout.EAST);
            }

            add(navigationPanel, BorderLayout.NORTH);

            // --- Main Content Panel ---
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setOpaque(false);
            contentPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

            JPanel heroSection = new JPanel(new BorderLayout());
            heroSection.setOpaque(false);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            JLabel mainTitle = new JLabel("Your Health Is");
            mainTitle.setFont(new Font("Arial", Font.BOLD, 48));
            mainTitle.setForeground(Color.WHITE);
            mainTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel subTitle = new JLabel("Our Priority.");
            subTitle.setFont(new Font("SansSerif", Font.BOLD, 48));
            subTitle.setForeground(new Color(100, 149, 237));
            subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel description = new JLabel("<html>Comprehensive healthcare services with experienced doctors<br>" +
                    "and state-of-the-art facilities. Your health is our mission<br>" +
                    "and we're committed to providing quality care.</html>");
            description.setFont(new Font("SansSerif", Font.PLAIN, 16));
            description.setForeground(new Color(220, 220, 220));
            description.setAlignmentX(Component.LEFT_ALIGNMENT);
            description.setBorder(new EmptyBorder(20, 0, 30, 0));

            JButton heroButton = new JButton("Book An Appointment");
            stylePrimaryButton(heroButton);
            heroButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            heroButton.addActionListener(e -> HospitalManagementSystem.showPage("AppointmentBookingPage1"));
            heroButton.setAlignmentX(Component.LEFT_ALIGNMENT);

            textPanel.add(mainTitle);
            textPanel.add(subTitle);
            textPanel.add(description);
            textPanel.add(heroButton);

            heroSection.add(textPanel, BorderLayout.WEST);
            contentPanel.add(heroSection, BorderLayout.CENTER);

            // --- Feature Services Panel (CHANGED TO 3 SERVICES - REMOVED PATIENT PORTAL) ---
            JPanel servicesPanel = new JPanel(new GridLayout(1, 3, 30, 0));
            servicesPanel.setOpaque(false);
            servicesPanel.setBorder(new EmptyBorder(60, 0, 0, 0));

            JPanel service1 = createServiceBox("📅", "Book Appointment",
                    "Schedule your visit with one of our specialists.",
                    () -> HospitalManagementSystem.showPage("AppointmentBookingPage1"));

            // REMOVED: Patient Portal service box

            JPanel service2 = createServiceBox("💳", "Online Bill Pay",
                    "Securely pay for hospital bills and services online.",
                    () -> HospitalManagementSystem.showPage("OnlinePaymentPage"));

            JPanel service3 = createServiceBox("💊", "Online Pharmacy",
                    "Order medicines and health products for delivery.",
                    () -> HospitalManagementSystem.showPage("OnlinePharmacyPage"));

            servicesPanel.add(service1);
            servicesPanel.add(service2);
            servicesPanel.add(service3);

            contentPanel.add(servicesPanel, BorderLayout.SOUTH);
            add(contentPanel, BorderLayout.CENTER);
            add(createScrollingBanner(), BorderLayout.SOUTH);
        }

        // Keep all your existing methods unchanged
        private JPanel createServiceBox(String icon, String title, String description, Runnable action) {
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBackground(new Color(255, 255, 255, 240));
            box.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(230, 230, 230), 1),
                    new EmptyBorder(25, 20, 25, 20)
            ));
            box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            box.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) { action.run(); }
                @Override
                public void mouseEntered(MouseEvent e) { box.setBackground(new Color(248, 249, 250)); }
                @Override
                public void mouseExited(MouseEvent e) { box.setBackground(new Color(255, 255, 255, 240)); }
            });

            JPanel iconPanel = new JPanel(new BorderLayout());
            iconPanel.setOpaque(false);
            iconPanel.setPreferredSize(new Dimension(60, 60));
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setOpaque(true);
            iconLabel.setBackground(new Color(30, 136, 229, 30));
            iconPanel.add(iconLabel);
            iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            titleLabel.setForeground(new Color(51, 51, 51));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setBorder(new EmptyBorder(15, 0, 10, 0));

            JLabel descLabel = new JLabel("<html><center>" + description + "</center></html>");
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            descLabel.setForeground(new Color(120, 120, 120));
            descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            box.add(iconPanel);
            box.add(titleLabel);
            box.add(descLabel);
            return box;
        }

        private JPanel createScrollingBanner() {
            JPanel bannerPanel = new JPanel(null);
            bannerPanel.setOpaque(false);
            bannerPanel.setPreferredSize(new Dimension(0, 50));
            bannerPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

            scrollingLabel = new JLabel("📞 24/7 Emergency Helpline: 1800-SALVE-HELP | 🆓 Free Consultation Call Now! | 🚑 Ambulance Service Available |");
            scrollingLabel.setForeground(Color.WHITE);
            scrollingLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            scrollingLabel.setOpaque(false);

            Dimension labelSize = scrollingLabel.getPreferredSize();
            scrollingLabel.setBounds(0, 15, labelSize.width, 20);
            bannerPanel.add(scrollingLabel);
            startScrollingAnimation();
            return bannerPanel;
        }

        private void startScrollingAnimation() {
            scrollTimer = new Timer(50, e -> {
                if (scrollingLabel != null) {
                    Container parent = scrollingLabel.getParent();
                    if (parent != null && parent.getWidth() > 0) {
                        int parentWidth = parent.getWidth();
                        int labelWidth = scrollingLabel.getPreferredSize().width;
                        scrollPosition -= 2;
                        if (scrollPosition + labelWidth < 0) {
                            scrollPosition = parentWidth;
                        }
                        scrollingLabel.setLocation(scrollPosition, 15);
                        parent.repaint();
                    }
                }
            });

            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    if (scrollTimer != null && !scrollTimer.isRunning()) {
                        scrollPosition = getWidth();
                        scrollTimer.start();
                    }
                }

                @Override
                public void componentHidden(java.awt.event.ComponentEvent e) {
                    if (scrollTimer != null && scrollTimer.isRunning()) {
                        scrollTimer.stop();
                    }
                }
            });
        }

        private void stylePrimaryButton(JButton button) {
            button.setBackground(new Color(220, 53, 69));
            button.setForeground(Color.WHITE);
            button.setFont(new Font("SansSerif", Font.BOLD, 14));
            button.setFocusPainted(false);
            button.setBorder(new EmptyBorder(12, 25, 12, 25));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    public static class HospitalMainPage extends JPanel {
        public HospitalMainPage() {
            setOpaque(false);
            setLayout(new GridBagLayout());

            JPanel loginPanel = new JPanel();
            loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
            loginPanel.setBackground(new Color(255, 255, 255, 220));
            loginPanel.setBorder(new EmptyBorder(50, 80, 50, 80));

            JLabel titleLabel = new JLabel("🏥 SALVE Hospital Staff Portal");
            titleLabel.setFont(new Font("Serif", Font.BOLD, 36));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setForeground(COLOR_FONT_DARK);

            JLabel subtitleLabel = new JLabel("Please select your role to log in.");
            subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subtitleLabel.setBorder(new EmptyBorder(10, 0, 30, 0));
            subtitleLabel.setForeground(new Color(100, 100, 100));

            // Network Status Indicator
            JPanel statusPanel = createNetworkStatusPanel();
            statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            loginPanel.add(titleLabel);
            loginPanel.add(subtitleLabel);
            loginPanel.add(statusPanel);
            loginPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            try {
                InetAddress address = InetAddress.getLocalHost();
                String ipAddress = address.getHostAddress();
                String macAddress = getMacAddress();

                // Hospital network - check MAC for admin vs doctor
                if (isHospitalIP(ipAddress)) {
                    if (isAdminMacAddress(macAddress)) {
                        // ADMIN MAC - Show ONLY Admin Login button
                        JButton adminLoginButton = new JButton("Admin Login");
                        styleLoginButton(adminLoginButton);
                        adminLoginButton.addActionListener(e -> HospitalManagementSystem.showPage("AdminLoginPage"));
                        loginPanel.add(adminLoginButton);
                    } else {
                        // DOCTOR/STAFF MAC - Show ONLY Doctor Login
                        JButton doctorLoginButton = new JButton("Doctor Login");
                        styleLoginButton(doctorLoginButton);
                        doctorLoginButton.addActionListener(e -> HospitalManagementSystem.showPage("DoctorLoginPage"));
                        loginPanel.add(doctorLoginButton);
                        // Change subtitle for doctors
                        subtitleLabel.setText("Welcome, Doctor. Please log in to continue.");
                    }
                } else {
                    // External network - show both buttons
                    JButton adminLoginButton = new JButton("Admin Login");
                    styleLoginButton(adminLoginButton);
                    adminLoginButton.addActionListener(e -> HospitalManagementSystem.showPage("AdminLoginPage"));
                    loginPanel.add(adminLoginButton);

                    loginPanel.add(Box.createRigidArea(new Dimension(0, 15)));

                    JButton doctorLoginButton = new JButton("Doctor Login");
                    styleLoginButton(doctorLoginButton);
                    doctorLoginButton.addActionListener(e -> HospitalManagementSystem.showPage("DoctorLoginPage"));
                    loginPanel.add(doctorLoginButton);
                }
            } catch (Exception e) {
                // Error getting IP - show admin login as fallback
                JButton adminLoginButton = new JButton("Admin Login");
                styleLoginButton(adminLoginButton);
                adminLoginButton.addActionListener(ev -> HospitalManagementSystem.showPage("AdminLoginPage"));
                loginPanel.add(adminLoginButton);
            }
            add(loginPanel);
        }

        private void styleLoginButton(JButton button) {
            button.setFont(new Font("SansSerif", Font.BOLD, 18));
            button.setBackground(COLOR_PRIMARY);
            button.setForeground(Color.WHITE);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setMaximumSize(new Dimension(300, 60));
            button.setPreferredSize(new Dimension(300, 60));

            button.addMouseListener(new MouseAdapter() {
                private Color originalColor = button.getBackground();

                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(COLOR_SECONDARY);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(originalColor);
                }
            });
        }

        private JPanel createNetworkStatusPanel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            panel.setOpaque(true);
            panel.setBackground(new Color(232, 245, 233));
            panel.setBorder(new EmptyBorder(10, 15, 10, 15));

            boolean isHospitalNetwork = isConnectedToHospitalNetwork();

            JLabel statusIcon = new JLabel(isHospitalNetwork ? "✅" : "⚠️");
            JLabel statusText = new JLabel(isHospitalNetwork ?
                    "Connected to Hospital Network" :
                    "External Network - Limited Access");

            statusText.setFont(new Font("SansSerif", Font.BOLD, 12));
            statusText.setForeground(isHospitalNetwork ?
                    new Color(46, 125, 50) :
                    new Color(255, 111, 0));

            panel.add(statusIcon);
            panel.add(statusText);
            return panel;
        }
    }


    public static class LoginPage extends JPanel {
        public LoginPage() {
            setOpaque(false);
            setLayout(new GridBagLayout());

            JPanel loginPanel = new JPanel();
            loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
            loginPanel.setBackground(new Color(255, 255, 255, 220));
            loginPanel.setBorder(new EmptyBorder(50, 80, 50, 80));

            // Title
            JLabel titleLabel = new JLabel("Select Your Role");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setForeground(COLOR_FONT_DARK);
            loginPanel.add(titleLabel);
            loginPanel.add(Box.createRigidArea(new Dimension(0, 30)));

            // Check current user's IP to determine what login buttons to show
            try {
                InetAddress address = InetAddress.getLocalHost();
                String ipAddress = address.getHostAddress();
                String macAddress = getMacAddress();

                // Hospital network - check MAC for admin vs doctor
                if (isHospitalIP(ipAddress)) {
                    if (isAdminMacAddress(macAddress)) {
                        // ADMIN MAC - Show ONLY Admin Login button WITH back button
                        JButton adminLoginButton = createStyledButton("Admin Login",
                                e -> HospitalManagementSystem.showPage("AdminLoginPage"));
                        loginPanel.add(adminLoginButton);

                        // Add back button for admin
                        addBackButton(loginPanel);

                    } else {
                        // DOCTOR MAC - Show ONLY Doctor Login button WITHOUT back button
                        JButton doctorLoginButton = createStyledButton("Doctor Login",
                                e -> HospitalManagementSystem.showPage("DoctorLoginPage"));
                        loginPanel.add(doctorLoginButton);
                        // NO BACK BUTTON FOR DOCTORS
                    }
                } else {
                    // External network - show both buttons WITH back button
                    JButton adminLoginButton = createStyledButton("Admin Login",
                            e -> HospitalManagementSystem.showPage("AdminLoginPage"));
                    loginPanel.add(adminLoginButton);
                    loginPanel.add(Box.createRigidArea(new Dimension(0, 15)));

                    JButton doctorLoginButton = createStyledButton("Doctor Login",
                            e -> HospitalManagementSystem.showPage("DoctorLoginPage"));
                    loginPanel.add(doctorLoginButton);

                    // Add back button for external network
                    addBackButton(loginPanel);
                }
            } catch (Exception e) {
                // Error getting IP - show admin login as fallback WITH back button
                JButton adminLoginButton = createStyledButton("Admin Login",
                        e2 -> HospitalManagementSystem.showPage("AdminLoginPage"));
                loginPanel.add(adminLoginButton);

                // Add back button for fallback
                addBackButton(loginPanel);
            }

            // REMOVED: The old back button code that was here (lines 60-91)

            add(loginPanel);
        }

        // Helper method to add back button (avoids code duplication)
        private void addBackButton(JPanel panel) {
            panel.add(Box.createRigidArea(new Dimension(0, 20)));

            JButton backButton = new JButton("← Back");
            backButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
            backButton.setBackground(Color.WHITE);
            backButton.setForeground(COLOR_PRIMARY);
            backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            backButton.setFocusPainted(false);
            backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            backButton.setMaximumSize(new Dimension(300, 40));
            backButton.setPreferredSize(new Dimension(300, 40));
            backButton.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(COLOR_PRIMARY, 2),
                    new EmptyBorder(5, 15, 5, 15)
            ));

            backButton.addActionListener(e -> HospitalManagementSystem.showPage("MainPage"));

            backButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    backButton.setBackground(new Color(240, 240, 240));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    backButton.setBackground(Color.WHITE);
                }
            });

            panel.add(backButton);
        }

        private JButton createStyledButton(String text, ActionListener actionListener) {
            JButton button = new JButton(text);
            button.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("SansSerif", Font.BOLD, 16));
            button.setBorder(new EmptyBorder(15, 25, 15, 25));
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(300, 50));
            button.setPreferredSize(new Dimension(300, 50));
            button.addActionListener(actionListener);
            return button;
        }
    }

    public static class AdminLoginPage extends JPanel {
        private final JTextField userField;
        private final JPasswordField passField;

        public AdminLoginPage() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(255, 255, 255, 230));
            formPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel title = new JLabel("Admin Login");
            title.setFont(new Font("SansSerif", Font.BOLD, 28));
            gbc.gridwidth = 2;
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(title, gbc);

            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Username:"), gbc);
            gbc.gridy = 2;
            formPanel.add(new JLabel("Password:"), gbc);

            gbc.anchor = GridBagConstraints.WEST;
            userField = new JTextField(15);
            userField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridx = 1;
            gbc.gridy = 1;
            formPanel.add(userField, gbc);

            passField = new JPasswordField(15);
            passField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridy = 2;
            formPanel.add(passField, gbc);

            JButton loginButton = new JButton("Login");
            loginButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            loginButton.setForeground(Color.WHITE);
            loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            loginButton.addActionListener(e -> performLogin());
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(loginButton, gbc);

            // Change the back button:
            JButton backButton = new JButton("← Back to Login Portal");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.setForeground(Color.BLACK);
            backButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("LoginPage"));
            gbc.gridy = 4;  // Next row
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(backButton, gbc);

            add(formPanel, new GridBagConstraints());
        }

        private void performLogin() {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            String role = HospitalManagementSystem.getDbManager().validateUser(username, password);
            if ("Admin".equals(role)) {
                HospitalManagementSystem.setCurrentAdminUser(username);
                HospitalManagementSystem.showPage("AdminPage");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid admin credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static class DoctorLoginPage extends JPanel {
        private final JTextField userField;
        private final JPasswordField passField;

        public DoctorLoginPage() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(255, 255, 255, 230));
            formPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel title = new JLabel("Doctor Login");
            title.setFont(new Font("SansSerif", Font.BOLD, 28));
            gbc.gridwidth = 2;
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(title, gbc);

            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Username:"), gbc);
            gbc.gridy = 2;
            formPanel.add(new JLabel("Password:"), gbc);

            gbc.anchor = GridBagConstraints.WEST;
            userField = new JTextField(15);
            userField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridx = 1;
            gbc.gridy = 1;
            formPanel.add(userField, gbc);

            passField = new JPasswordField(15);
            passField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridy = 2;
            formPanel.add(passField, gbc);

            JButton loginButton = new JButton("Login");
            loginButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            loginButton.setForeground(Color.WHITE);
            loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            loginButton.addActionListener(e -> performLogin());
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(loginButton, gbc);
            JButton backButton = new JButton("← Back to Staff Portal");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.setForeground(Color.BLACK);
            backButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("HospitalMainPage"));
            gbc.gridy = 4;  // Next row
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(backButton, gbc);

            add(formPanel, new GridBagConstraints());
        }

        private void performLogin() {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            String role = HospitalManagementSystem.getDbManager().validateUser(username, password);
            if ("Doctor".equals(role)) {
                Doctor doctor = HospitalManagementSystem.getDbManager().getDoctorByUsername(username);
                if (doctor != null) {
                    HospitalManagementSystem.getDbManager().recordDoctorLogin(doctor.getId());
                    HospitalManagementSystem.setCurrentDoctorUser(doctor);
                    HospitalManagementSystem.showPage("DoctorPage");
                } else {
                    JOptionPane.showMessageDialog(this, "Doctor profile not found for this user.", "Login Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid doctor credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static class PatientLoginPage extends JPanel {
        private final JTextField nameField;
        private final JTextField idField;
        // REMOVED: private final JTextField ageField;

        public PatientLoginPage() {
            setOpaque(false);
            setLayout(new GridBagLayout());

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(255, 255, 255, 230));
            formPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            // Title
            JLabel title = new JLabel("Patient Portal Login");
            title.setFont(new Font("SansSerif", Font.BOLD, 28));
            gbc.gridwidth = 2;
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(title, gbc);

            // Labels (left column)
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;

            gbc.gridy = 1;
            formPanel.add(new JLabel("Patient Name:"), gbc);

            gbc.gridy = 2;
            formPanel.add(new JLabel("Patient ID:"), gbc);

            // REMOVED: Age label and field

            // Input fields (right column)
            gbc.anchor = GridBagConstraints.WEST;

            nameField = new JTextField(15);
            nameField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridx = 1;
            gbc.gridy = 1;
            formPanel.add(nameField, gbc);

            idField = new JTextField(15);
            idField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridy = 2;
            formPanel.add(idField, gbc);

            // REMOVED: ageField components

            // Buttons
            JButton loginButton = new JButton("Access Portal");
            loginButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            loginButton.setForeground(Color.WHITE);
            loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            loginButton.addActionListener(e -> performLogin());
            gbc.gridy = 3;  // Back to 3 (was 4)
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(loginButton, gbc);

            JButton backButton = new JButton("← Back");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("MainPage"));
            gbc.gridy = 4;  // Back to 4 (was 5)
            formPanel.add(backButton, gbc);

            add(formPanel, new GridBagConstraints());
        }

        private void performLogin() {
            String name = nameField.getText().trim();
            String idText = idField.getText().trim();
            // REMOVED: String ageText = ageField.getText().trim();

            // Updated validation to remove age requirement
            if (name.isEmpty() || idText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter patient name and ID.",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!name.matches("[a-zA-Z ]+")) {
                JOptionPane.showMessageDialog(this,
                        "Patient name should contain only letters and spaces.",
                        "Invalid Name",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int patientId = Integer.parseInt(idText);
                // REMOVED: age validation

                Patient patient = HospitalManagementSystem.getDbManager().getPatientByNameAndId(name, patientId);
                if (patient != null) {
                    HospitalManagementSystem.showPatientInfoPage(patient);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Patient not found. Please check your name and ID.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid number for Patient ID.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static class EnhancedOnlinePaymentPage extends JPanel {
        private JTextField patientIdField, amountField, cardNameField, cardNumberField, cvvField, upiIdField;
        private CardLayout paymentMethodLayout;
        private JPanel paymentMethodPanel;
        private JComboBox<String> monthCombo, yearCombo;
        private JLabel expiryDisplay, cardNumberDisplay, nameDisplay, uploadedImageLabel, orderSummaryLabel;
        private final Map<Medicine, Integer> pharmacyCart;
        private final String deliveryAddress;
        private final boolean isPharmacyPayment;
        private final double predefinedAmount;

        public EnhancedOnlinePaymentPage() {
            this(null, null, 0.0, false);
        }

        public EnhancedOnlinePaymentPage(Map<Medicine, Integer> cart, String address, double amount, boolean isPharmacy) {
            this.pharmacyCart = cart;
            this.deliveryAddress = address;
            this.predefinedAmount = amount;
            this.isPharmacyPayment = isPharmacy;

            setOpaque(false);
            setLayout(new BorderLayout(20, 20));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 20));
            mainPanel.setOpaque(true);

            JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
            leftPanel.setOpaque(false);
            TitledBorder paymentBorder = BorderFactory.createTitledBorder(isPharmacyPayment ? "Pharmacy Payment Information" : "Hospital Payment Information");
            paymentBorder.setTitleFont(new Font("Serif", Font.BOLD, 28));
            paymentBorder.setTitleJustification(TitledBorder.CENTER);
            leftPanel.setBorder(paymentBorder);

            JPanel casePanel = new JPanel(new GridBagLayout());
            casePanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 10, 5, 10);

            if (isPharmacyPayment) {
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.EAST;
                casePanel.add(new JLabel("Delivery Address:"), gbc);
                JTextArea addressArea = new JTextArea(address, 3, 20);
                addressArea.setEditable(false);
                addressArea.setBackground(Color.LIGHT_GRAY);
                addressArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
                gbc.gridx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                casePanel.add(new JScrollPane(addressArea), gbc);

                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.anchor = GridBagConstraints.EAST;
                casePanel.add(new JLabel("Total Amount (INR):"), gbc);
                amountField = new JTextField(String.format("%.2f", amount), 10);
                amountField.setEditable(false);
                amountField.setBackground(Color.LIGHT_GRAY);
                gbc.gridx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                casePanel.add(amountField, gbc);

                patientIdField = new JTextField(10);
                patientIdField.setVisible(false);
            } else {
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.EAST;
                casePanel.add(new JLabel("Patient ID:"), gbc);
                patientIdField = new JTextField(10);
                gbc.gridx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                casePanel.add(patientIdField, gbc);

                amountField = new JTextField();
                amountField.setVisible(false);
                patientIdField.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        updateOrderSummaryFromPatientId();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        updateOrderSummaryFromPatientId();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        updateOrderSummaryFromPatientId();
                    }
                });
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.anchor = GridBagConstraints.EAST;
                casePanel.add(new JLabel("Upload Prescription:"), gbc);
                JButton uploadButton = new JButton("Choose File");
                gbc.gridx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                casePanel.add(uploadButton, gbc);
                uploadButton.addActionListener(e -> uploadImage());
            }
            leftPanel.add(casePanel, BorderLayout.NORTH);

            JPanel methodTabPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            JButton cardButton = new JButton("Credit/Debit Card");
            JButton paypalButton = new JButton("Pay with PayPal");
            JButton upiButton = new JButton("UPI Payment");
            methodTabPanel.add(cardButton);
            methodTabPanel.add(paypalButton);
            methodTabPanel.add(upiButton);
            leftPanel.add(methodTabPanel, BorderLayout.CENTER);

            setupPaymentMethodPanels();
            leftPanel.add(paymentMethodPanel, BorderLayout.SOUTH);
            mainPanel.add(leftPanel);

            JPanel summaryPanel = new JPanel(new BorderLayout(10, 10));
            summaryPanel.setOpaque(true);
            TitledBorder orderBorder = BorderFactory.createTitledBorder("Order Summary");
            orderBorder.setTitleFont(new Font("Serif", Font.BOLD, 28));
            orderBorder.setTitleJustification(TitledBorder.CENTER);
            summaryPanel.setBorder(orderBorder);

            orderSummaryLabel = new JLabel();
            orderSummaryLabel.setVerticalAlignment(SwingConstants.TOP);
            summaryPanel.add(orderSummaryLabel, BorderLayout.CENTER);
            if (!isPharmacyPayment) {
                uploadedImageLabel = new JLabel("No prescription uploaded", SwingConstants.CENTER);
                uploadedImageLabel.setPreferredSize(new Dimension(200, 200));
                uploadedImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                summaryPanel.add(uploadedImageLabel, BorderLayout.SOUTH);
            } else {
                uploadedImageLabel = null;
            }
            mainPanel.add(summaryPanel);
            add(mainPanel, BorderLayout.CENTER);

            cardButton.addActionListener(e -> paymentMethodLayout.show(paymentMethodPanel, "Card"));
            paypalButton.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.paypal.com"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Unable to open PayPal website.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            upiButton.addActionListener(e -> paymentMethodLayout.show(paymentMethodPanel, "UPI"));

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.setOpaque(false);
            JButton payButton = new JButton(isPharmacyPayment ? "Complete Order" : "Pay Now");
            payButton.setBackground(new Color(46, 125, 50));
            payButton.setForeground(Color.WHITE);
            payButton.addActionListener(e -> processPayment());
            JButton backButton = new JButton("← " + (isPharmacyPayment ? "Back to Order" : "Back to Home"));
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> {
                if (isPharmacyPayment) {
                    HospitalManagementSystem.showPharmacyPaymentPage(pharmacyCart, deliveryAddress);
                } else {
                    HospitalManagementSystem.showPage("MainPage");
                }
            });
            bottomPanel.add(payButton);
            bottomPanel.add(backButton);
            add(bottomPanel, BorderLayout.SOUTH);

            if (isPharmacyPayment) {
                updatePharmacyOrderSummary();
            } else {
                orderSummaryLabel.setText("<html><i>Enter Patient ID to see billing details</i></html>");
            }
        }

        private void updateOrderSummaryFromPatientId() {
            if (isPharmacyPayment) {
                updatePharmacyOrderSummary();
                return;
            }

            String patientIdText = patientIdField.getText().trim();
            if (patientIdText.isEmpty()) {
                orderSummaryLabel.setText("<html><i>Enter Patient ID to see billing details</i></html>");
                amountField.setText("0.00");
                return;
            }

            try {
                int patientId = Integer.parseInt(patientIdText);
                Patient patient = HospitalManagementSystem.getDbManager().getPatientById(patientId);

                if (patient == null) {
                    orderSummaryLabel.setText("<html><span style='color: red;'>Patient ID " + patientId + " not found</span></html>");
                    amountField.setText("0.00");
                    return;
                }

                // FIXED LOGIC: Check if patient exists in consultations table
                List<Consultation> consultations = HospitalManagementSystem.getDbManager()
                        .getConsultationsForPatient(patientId);

                if (consultations.isEmpty()) {
                    // Patient is registered but NOT consulted yet
                    orderSummaryLabel.setText("<html><h3>Patient Information</h3><hr>" +
                            "<b>Patient:</b> " + patient.getName() + "<br>" +
                            "<b>ID:</b> " + patient.getId() + "<br>" +
                            "<b>Contact:</b> " + patient.getContactNumber() + "<br><br>" +
                            "<div style='color: #ff6b35; font-size: 14px; padding: 15px; border: 2px solid #ff6b35; background-color: #fff8f5; border-radius: 5px;'>" +
                            "<b>⚠️ Patient Registered but Not Consulted</b><br>" +
                            "This patient is registered in our system but has not been consulted by any doctor yet.<br>" +
                            "<b>Status:</b> Awaiting consultation<br>" +
                            "<b>Action Required:</b> Patient needs to book an appointment or walk-in for consultation before billing can be generated." +
                            "</div></html>");
                    amountField.setText("0.00");
                    return;
                }

                // Patient has consultations - now check if any are billable
                List<Consultation> billableConsultations = getBillableConsultations(consultations);

                if (billableConsultations.isEmpty()) {
                    orderSummaryLabel.setText("<html><h3>Patient Information</h3><hr>" +
                            "<b>Patient:</b> " + patient.getName() + "<br>" +
                            "<b>ID:</b> " + patient.getId() + "<br>" +
                            "<b>Contact:</b> " + patient.getContactNumber() + "<br><br>" +
                            "<div style='color: orange; font-size: 14px; padding: 10px; border: 1px solid orange; background-color: #fff3cd;'>" +
                            "<b>⏳ Consultation in Progress</b><br>" +
                            "Patient has scheduled consultations but none are completed yet.<br>" +
                            "Billing will be available after consultation completion." +
                            "</div></html>");
                    amountField.setText("0.00");
                    return;
                }

                // Calculate and display bill for completed consultations
                double calculatedAmount = calculatePatientBill(billableConsultations);
                amountField.setText(String.valueOf(calculatedAmount));
                updateOrderSummaryWithConsultations(patient, billableConsultations, calculatedAmount);

            } catch (NumberFormatException e) {
                orderSummaryLabel.setText("<html><span style='color: red;'>Please enter a valid Patient ID (numbers only)</span></html>");
                amountField.setText("0.00");
            }
        }

        // Helper method to get billable consultations
        private List<Consultation> getBillableConsultations(List<Consultation> consultations) {
            List<Consultation> billableConsultations = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Consultation consultation : consultations) {

                boolean isTimeCompleted = consultation.getConsultationDateTime().isBefore(now);

                // Check for medical content
                List<Prescription> prescriptions = HospitalManagementSystem.getDbManager()
                        .getPrescriptionsForConsultation(consultation.getId());
                List<MedicalTest> tests = HospitalManagementSystem.getDbManager()
                        .getTestsForConsultation(consultation.getId());
                boolean hasMedicalContent = !prescriptions.isEmpty() || !tests.isEmpty();

                // Billable if time completed OR has medical content
                if (isTimeCompleted || hasMedicalContent) {
                    billableConsultations.add(consultation);
                }
            }

            return billableConsultations;
        }

        private double calculatePatientBill(List<Consultation> billableConsultations) {
            double totalBill = 0.0;

            for (Consultation consultation : billableConsultations) {
                double consultationFee = 500.0; // Base consultation fee
                double medicineCost = 0.0;
                double testCost = 0.0;

                // Add prescription costs
                List<Prescription> prescriptions = HospitalManagementSystem.getDbManager()
                        .getPrescriptionsForConsultation(consultation.getId());
                medicineCost = prescriptions.size() * 75.0; // 75 per medicine

                // Add test costs
                List<MedicalTest> tests = HospitalManagementSystem.getDbManager()
                        .getTestsForConsultation(consultation.getId());
                for (MedicalTest test : tests) {
                    testCost += test.getPrice();
                }

                // Apply specialist consultation premium
                Doctor doctor = HospitalManagementSystem.getDbManager().getDoctorById(consultation.getDoctorId());
                if (doctor != null) {
                    if ("Cardiology".equals(doctor.getSpecialization())) {
                        consultationFee += 300.0; // Premium for cardiology
                    } else if ("Pediatrics".equals(doctor.getSpecialization())) {
                        consultationFee += 150.0; // Premium for pediatrics
                    } else if ("Orthopedics".equals(doctor.getSpecialization())) {
                        consultationFee += 200.0; // Premium for orthopedics
                    }
                }

                totalBill += consultationFee + medicineCost + testCost;
            }

            return Math.max(totalBill, 300.0); // Minimum bill amount
        }

        private void updateOrderSummaryWithConsultations(Patient patient, List<Consultation> consultations, double amount) {
            StringBuilder sb = new StringBuilder("<html>");
            sb.append("<h3>Patient Billing Details</h3><hr>");
            sb.append("<b>Patient:</b> ").append(patient.getName()).append("<br>");
            sb.append("<b>ID:</b> ").append(patient.getId()).append("<br>");
            sb.append("<b>Contact:</b> ").append(patient.getContactNumber()).append("<br><br>");

            if (!consultations.isEmpty()) {
                // Show most recent consultation details
                Consultation recent = consultations.get(consultations.size() - 1);
                Doctor doctor = HospitalManagementSystem.getDbManager().getDoctorById(recent.getDoctorId());

                sb.append("<b>Recent Consultation:</b><br>");
                sb.append("Doctor: Dr. ").append(doctor != null ? doctor.getName() : "Unknown").append("<br>");

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                sb.append("Date: ").append(recent.getConsultationDateTime().format(dateFormatter)).append("<br>");
                sb.append("Reason: ").append(recent.getConsultingReason()).append("<br><br>");

                // Calculate totals
                double totalConsultationFee = 0.0;
                double totalMedicineCost = 0.0;
                double totalTestCost = 0.0;

                // Store all prescribed medicines and tests for display
                List<Prescription> allPrescriptions = new ArrayList<>();
                List<MedicalTest> allTests = new ArrayList<>();

                for (Consultation consultation : consultations) {
                    totalConsultationFee += 500.0; // Base fee per consultation

                    // Add specialist premium
                    Doctor consultationDoctor = HospitalManagementSystem.getDbManager()
                            .getDoctorById(consultation.getDoctorId());
                    if (consultationDoctor != null) {
                        if ("Cardiology".equals(consultationDoctor.getSpecialization())) {
                            totalConsultationFee += 300.0;
                        } else if ("Pediatrics".equals(consultationDoctor.getSpecialization())) {
                            totalConsultationFee += 150.0;
                        } else if ("Orthopedics".equals(consultationDoctor.getSpecialization())) {
                            totalConsultationFee += 200.0;
                        }
                    }

                    // Collect prescribed medicines
                    List<Prescription> prescriptions = HospitalManagementSystem.getDbManager()
                            .getPrescriptionsForConsultation(consultation.getId());
                    allPrescriptions.addAll(prescriptions);
                    totalMedicineCost += prescriptions.size() * 75.0;

                    // Collect prescribed tests
                    List<MedicalTest> tests = HospitalManagementSystem.getDbManager()
                            .getTestsForConsultation(consultation.getId());
                    allTests.addAll(tests);
                    for (MedicalTest test : tests) {
                        totalTestCost += test.getPrice();
                    }
                }

                // Show prescribed medicines with details
                if (!allPrescriptions.isEmpty()) {
                    sb.append("<b>Prescribed Medicines:</b><br>");
                    for (Prescription prescription : allPrescriptions) {
                        sb.append("  • ").append(prescription.getMedicineName())
                                .append(" (").append(prescription.getDosage()).append(") - ₹75.00<br>");
                    }
                    sb.append("<br>");
                }

                // Show prescribed tests
                if (!allTests.isEmpty()) {
                    sb.append("<b>Prescribed Tests:</b><br>");
                    for (MedicalTest test : allTests) {
                        sb.append("  • ").append(test.getName())
                                .append(" - ₹").append(String.format("%.2f", test.getPrice())).append("<br>");
                    }
                    sb.append("<br>");
                }

                // Show bill breakdown
                sb.append("<hr><b>Bill Breakdown:</b><br>");
                sb.append("Consultation Fee: ₹").append(String.format("%.2f", totalConsultationFee)).append("<br>");

                if (totalMedicineCost > 0) {
                    sb.append("Medicines (").append(allPrescriptions.size()).append(" items): ₹")
                            .append(String.format("%.2f", totalMedicineCost)).append("<br>");
                }

                if (totalTestCost > 0) {
                    sb.append("Medical Tests (").append(allTests.size()).append(" items): ₹")
                            .append(String.format("%.2f", totalTestCost)).append("<br>");
                }

                sb.append("<hr>");
                sb.append("<b style='font-size: 16px; color: #2e7d32;'>Total Amount: ₹")
                        .append(String.format("%.2f", amount)).append("</b><br><br>");
                sb.append("<small style='color: #666;'>All prices are inclusive of applicable taxes</small>");
            }

            sb.append("</html>");
            orderSummaryLabel.setText(sb.toString());
        }

        private void setupPaymentMethodPanels() {
            paymentMethodLayout = new CardLayout();
            paymentMethodPanel = new JPanel(paymentMethodLayout);
            paymentMethodPanel.setOpaque(true);

            // Card panel
            JPanel cardPanel = new JPanel(new GridBagLayout());
            cardPanel.setOpaque(true);
            cardPanel.setName("Card");
            int row = 0;
            JPanel cardGraphicPanel = new JPanel(null);
            cardGraphicPanel.setBackground(new Color(51, 51, 51));
            cardGraphicPanel.setPreferredSize(new Dimension(470, 170));
            cardGraphicPanel.setBorder(new LineBorder(new Color(51, 51, 51), 2, true));
            JLabel chipLabel = new JLabel("🔑");
            chipLabel.setForeground(Color.WHITE);
            chipLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            chipLabel.setBounds(20, 25, 40, 40);
            cardGraphicPanel.add(chipLabel);
            cardNumberDisplay = new JLabel("•••• •••• •••• ••••");
            cardNumberDisplay.setFont(new Font("SansSerif", Font.BOLD, 24));
            cardNumberDisplay.setForeground(Color.WHITE);
            cardNumberDisplay.setBounds(40, 70, 350, 30);
            cardGraphicPanel.add(cardNumberDisplay);
            nameDisplay = new JLabel("YOUR NAME");
            nameDisplay.setFont(new Font("SansSerif", Font.BOLD, 16));
            nameDisplay.setForeground(Color.WHITE);
            nameDisplay.setBounds(40, 110, 200, 25);
            cardGraphicPanel.add(nameDisplay);
            expiryDisplay = new JLabel("MM/YY");
            expiryDisplay.setFont(new Font("SansSerif", Font.BOLD, 14));
            expiryDisplay.setForeground(Color.WHITE);
            expiryDisplay.setBounds(350, 140, 70, 20);
            cardGraphicPanel.add(expiryDisplay);
            JLabel cardTypeDisplay = new JLabel("Credit Card");
            cardTypeDisplay.setFont(new Font("SansSerif", Font.BOLD, 14));
            cardTypeDisplay.setForeground(Color.WHITE);
            cardTypeDisplay.setBounds(320, 30, 110, 20);
            cardGraphicPanel.add(cardTypeDisplay);

            GridBagConstraints cbg = new GridBagConstraints();
            cbg.gridx = 0;
            cbg.gridy = row++;
            cbg.gridwidth = 2;
            cbg.insets = new Insets(10, 10, 20, 10);
            cbg.anchor = GridBagConstraints.NORTH;
            cardPanel.add(cardGraphicPanel, cbg);

            cbg.gridwidth = 1;
            cbg.gridx = 0;
            cbg.gridy = row;
            cbg.anchor = GridBagConstraints.EAST;
            cardPanel.add(new JLabel("Cardholder Name:"), cbg);
            cardNameField = new JTextField(18);
            cbg.gridx = 1;
            cbg.anchor = GridBagConstraints.WEST;
            cardPanel.add(cardNameField, cbg);

            cbg.gridx = 0;
            cbg.gridy = ++row;
            cbg.anchor = GridBagConstraints.EAST;
            cardPanel.add(new JLabel("Card Number:"), cbg);
            cardNumberField = new JTextField(18);
            cbg.gridx = 1;
            cbg.anchor = GridBagConstraints.WEST;
            cardPanel.add(cardNumberField, cbg);

            cardNumberField.getDocument().addDocumentListener(createCardDisplayUpdater());
            cardNameField.getDocument().addDocumentListener(createNameUpdater());

            cbg.gridx = 0;
            cbg.gridy = ++row;
            cbg.anchor = GridBagConstraints.EAST;
            cardPanel.add(new JLabel("Month:"), cbg);
            monthCombo = new JComboBox<>();
            for (int i = 1; i <= 12; i++) monthCombo.addItem(String.format("%02d", i));
            cbg.gridx = 1;
            cbg.anchor = GridBagConstraints.WEST;
            cardPanel.add(monthCombo, cbg);
            cbg.gridx = 0;
            cbg.gridy = ++row;
            cbg.anchor = GridBagConstraints.EAST;
            cardPanel.add(new JLabel("Year:"), cbg);
            yearCombo = new JComboBox<>();
            int thisYear = java.time.Year.now().getValue();
            for (int i = thisYear; i <= thisYear + 15; i++) yearCombo.addItem(String.valueOf(i));
            cbg.gridx = 1;
            cbg.anchor = GridBagConstraints.WEST;
            cardPanel.add(yearCombo, cbg);
            monthCombo.addActionListener(e -> updateExpiry());
            yearCombo.addActionListener(e -> updateExpiry());

            cbg.gridx = 0;
            cbg.gridy = ++row;
            cbg.anchor = GridBagConstraints.EAST;
            cardPanel.add(new JLabel("CVV:"), cbg);
            cvvField = new JTextField(5);
            cbg.gridx = 1;
            cbg.anchor = GridBagConstraints.WEST;
            cardPanel.add(cvvField, cbg);
            paymentMethodPanel.add(cardPanel, "Card");

            // UPI panel
            JPanel upiPanel = new JPanel(new GridBagLayout());
            upiPanel.setOpaque(true);
            upiPanel.setName("UPI");
            GridBagConstraints ugbc = new GridBagConstraints();
            ugbc.insets = new Insets(10, 10, 10, 10);
            ugbc.gridx = 0;
            ugbc.gridy = 0;
            upiPanel.add(new JLabel("Enter your UPI ID:"), ugbc);
            upiIdField = new JTextField(20);
            ugbc.gridx = 1;
            upiPanel.add(upiIdField, ugbc);
            try {
                URL url = new URI("https://i.postimg.cc/VkTtX0xS/qrcode.png").toURL();
                BufferedImage qrImage = ImageIO.read(url);
                JLabel qrLabel = new JLabel(new ImageIcon(qrImage.getScaledInstance(180, 180, java.awt.Image.SCALE_SMOOTH)));
                GridBagConstraints qgbc = new GridBagConstraints();
                qgbc.gridx = 0;
                qgbc.gridy = 1;
                qgbc.gridwidth = 2;
                qgbc.anchor = GridBagConstraints.CENTER;
                qgbc.insets = new Insets(18, 10, 10, 10);
                upiPanel.add(qrLabel, qgbc);
                JLabel scanLabel = new JLabel("Or SCAN to PAY with any UPI app");
                scanLabel.setForeground(Color.BLACK);
                scanLabel.setHorizontalAlignment(SwingConstants.CENTER);
                qgbc.gridy = 2;
                upiPanel.add(scanLabel, qgbc);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            paymentMethodPanel.add(upiPanel, "UPI");
        }

        private DocumentListener createCardDisplayUpdater() {
            return new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    update();
                }

                public void removeUpdate(DocumentEvent e) {
                    update();
                }

                public void changedUpdate(DocumentEvent e) {
                    update();
                }

                private void update() {
                    String text = cardNumberField.getText().replaceAll("\\D", "");
                    if (text.length() > 16) text = text.substring(0, 16);
                    String display = text.replaceAll(".{4}", "$0 ").trim();
                    cardNumberDisplay.setText(display.isEmpty() ? "•••• •••• •••• ••••" : display);
                }
            };
        }

        private DocumentListener createNameUpdater() {
            return new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    update();
                }

                public void removeUpdate(DocumentEvent e) {
                    update();
                }

                public void changedUpdate(DocumentEvent e) {
                    update();
                }

                private void update() {
                    String name = cardNameField.getText().trim();
                    nameDisplay.setText(name.isEmpty() ? "YOUR NAME" : name);
                }
            };
        }

        private void updatePharmacyOrderSummary() {
            if (!isPharmacyPayment || pharmacyCart == null) return;
            StringBuilder sb = new StringBuilder("<html><h3>Pharmacy Order Details</h3>");
            double subtotal = 0;
            for (Map.Entry<Medicine, Integer> entry : pharmacyCart.entrySet()) {
                Medicine med = entry.getKey();
                int quantity = entry.getValue();
                double itemTotal = med.getPrice() * quantity;
                subtotal += itemTotal;
                sb.append(String.format("%s x%d<br>₹%.2f each = ₹%.2f<br><br>", med.getName(), quantity, med.getPrice(), itemTotal));
            }
            double deliveryCharges = 50.00, tax = subtotal * 0.05, total = subtotal + deliveryCharges + tax;
            sb.append("<hr>");
            sb.append(String.format("Subtotal: ₹%.2f<br>", subtotal));
            sb.append(String.format("Delivery: ₹%.2f<br>", deliveryCharges));
            sb.append(String.format("Tax (5%%): ₹%.2f<br>", tax));
            sb.append("<hr>");
            sb.append(String.format("<b>Total: ₹%.2f</b><br><br><small>Delivery Address:<br>%s</small></html>", total, deliveryAddress.replace(", ", "<br>")));
            orderSummaryLabel.setText(sb.toString());
        }

        private void processPayment() {
            if (isPharmacyPayment) {
                String message = String.format("Pharmacy Order Confirmed!\n\nTotal Amount: ₹%.2f\n\nYour medicines will be delivered to:\n%s\n\nEstimated delivery: 2-3 working days", predefinedAmount, deliveryAddress);
                JOptionPane.showMessageDialog(this, message, "Order Confirmed", JOptionPane.INFORMATION_MESSAGE);
                HospitalManagementSystem.showPage("MainPage");
            } else {
                String patientId = patientIdField.getText().trim();
                String amountStr = amountField.getText().trim();
                if (patientId.isEmpty() || amountStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a Patient ID.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    double typedAmount = Double.parseDouble(amountStr);
                    if (typedAmount <= 0) throw new NumberFormatException();
                    double total = typedAmount + (typedAmount * 0.18) - 50.0;
                    String message = "Payment Confirmed:\nPatient ID: " + patientId + "\nAmount: ₹" + String.format("%.2f", total);
                    String method = getVisibleCardName(paymentMethodPanel);
                    if ("Card".equals(method)) {
                        if (cardNameField.getText().trim().isEmpty() || cardNumberField.getText().replaceAll("\\D", "").length() != 16 || cvvField.getText().trim().length() < 3) {
                            JOptionPane.showMessageDialog(this, "Enter valid card details.", "Invalid Card", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        message += "\nPaid by Card for " + cardNameField.getText().trim();
                    } else if ("UPI".equals(method)) {
                        if (upiIdField.getText().trim().isEmpty()) {
                            JOptionPane.showMessageDialog(this, "Enter your UPI ID.", "UPI Required", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        message += "\nPaid by UPI: " + upiIdField.getText().trim();
                    }
                    JOptionPane.showMessageDialog(this, message, "Payment Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    patientIdField.setText("");
                    amountField.setText("");
                    cardNameField.setText("");
                    cardNumberField.setText("");
                    cvvField.setText("");
                    upiIdField.setText("");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid billing amount.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void uploadImage() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select an image file");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "png", "gif", "bmp"));
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage img = ImageIO.read(fileChooser.getSelectedFile());
                    if (uploadedImageLabel != null) {
                        uploadedImageLabel.setIcon(new ImageIcon(img.getScaledInstance(200, 200, Image.SCALE_SMOOTH)));
                        uploadedImageLabel.setText(null);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error reading the image file.", "File Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void updateExpiry() {
            if (monthCombo != null && yearCombo != null && expiryDisplay != null) {
                expiryDisplay.setText(monthCombo.getSelectedItem() + "/" + yearCombo.getSelectedItem().toString().substring(2));
            }
        }

        private String getVisibleCardName(JPanel container) {
            for (Component comp : container.getComponents()) if (comp.isVisible()) return comp.getName();
            return null;
        }
    }

    public static class AdminPage extends JPanel {
        public AdminPage(String adminUsername) {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(HospitalManagementSystem.COLOR_PRIMARY);

            JLabel welcomeLabel = new JLabel(" Welcome back, " + adminUsername + "!");
            welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            welcomeLabel.setForeground(Color.WHITE);
            topPanel.add(welcomeLabel, BorderLayout.WEST);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            buttonPanel.setOpaque(false);

            buttonPanel.add(createStyledButton("Patient Details",
                    e -> HospitalManagementSystem.showPage("PatientDetailsPageAdmin")));
            buttonPanel.add(createStyledButton("Upcoming Appointments",
                    e -> HospitalManagementSystem.showPage("AppointmentsViewPageAdmin")));
            buttonPanel.add(createStyledButton("Doctor Management",
                    e -> HospitalManagementSystem.showPage("DoctorManagementPage")));
            buttonPanel.add(createStyledButton("Import Doctor Schedule",
                    e -> HospitalManagementSystem.showPage("DoctorScheduleImportPage")));

            topPanel.add(buttonPanel, BorderLayout.CENTER);

            // Logout button
            JButton logoutButton = createStyledButton("Logout",
                    e -> HospitalManagementSystem.showPage("LoginPage"));
            logoutButton.setBackground(Color.RED);
            logoutButton.setForeground(Color.WHITE);

            JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            logoutPanel.setOpaque(false);
            logoutPanel.add(logoutButton);
            topPanel.add(logoutPanel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            JLabel centerMessage = new JLabel("Admin Dashboard", SwingConstants.CENTER);
            centerMessage.setFont(new Font("SansSerif", Font.BOLD, 48));
            centerMessage.setForeground(Color.WHITE);
            add(centerMessage, BorderLayout.CENTER);
        }

        private JButton createStyledButton(String text, ActionListener actionListener) {
            JButton button = new JButton(text);
            button.setBackground(HospitalManagementSystem.COLOR_SECONDARY);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBorder(new EmptyBorder(8, 15, 8, 15));
            button.addActionListener(actionListener);
            return button;
        }
    }
    public static class DoctorScheduleImportPage extends JPanel {
        private JLabel statusLabel;
        private JTextArea logArea;

        public DoctorScheduleImportPage() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            // Top Panel
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            topPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

            JLabel titleLabel = new JLabel("Doctor Schedule CSV Import");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            titleLabel.setForeground(Color.WHITE);
            topPanel.add(titleLabel, BorderLayout.WEST);

            JButton backButton = createStyledButton("Back to Admin", e ->
                    HospitalManagementSystem.showPage("AdminPage"));
            backButton.setBackground(Color.RED);
            backButton.setForeground(Color.WHITE);
            topPanel.add(backButton, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            // Main Content Panel
            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            // Instructions Panel
            JPanel instructionsPanel = new JPanel();
            instructionsPanel.setLayout(new BoxLayout(instructionsPanel, BoxLayout.Y_AXIS));
            instructionsPanel.setOpaque(false);
            instructionsPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

            JLabel instructionTitle = new JLabel("CSV File Format Instructions:");
            instructionTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
            instructionsPanel.add(instructionTitle);

            JLabel format1 = new JLabel("• Column 1: Doctor Name (e.g., 'Alice Smith')");
            JLabel format2 = new JLabel("• Column 2: Date in DD/MM/YYYY format (e.g., '15/11/2025')");
            JLabel format3 = new JLabel("• Column 3: Start Time in HH:MM:SS format (e.g., '09:00:00')");
            JLabel format4 = new JLabel("• Column 4: End Time in HH:MM:SS format (e.g., '17:00:00')");



            format1.setFont(new Font("SansSerif", Font.PLAIN, 14));
            format2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            format3.setFont(new Font("SansSerif", Font.PLAIN, 14));
            format4.setFont(new Font("SansSerif", Font.PLAIN, 14));

            instructionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            instructionsPanel.add(format1);
            instructionsPanel.add(format2);
            instructionsPanel.add(format3);
            instructionsPanel.add(format4);

            JLabel exampleLabel = new JLabel("Example CSV content:");
            exampleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            exampleLabel.setBorder(new EmptyBorder(15, 0, 5, 0));
            instructionsPanel.add(exampleLabel);
            JTextArea exampleArea = new JTextArea(
                    "doctorname,date,start_time,end_time\n" +
                            "Alice Smith,15/11/2025,09:00:00,17:00:00\n" +
                            "Bob Johnson,16/11/2025,10:00:00,18:00:00\n" +
                            "Alice Smith,17/11/2025,09:00:00,13:00:00"
            );
            exampleArea.setEditable(false);
            exampleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            exampleArea.setBackground(new Color(245, 245, 245));
            exampleArea.setBorder(new EmptyBorder(10, 10, 10, 10));
            instructionsPanel.add(exampleArea);

            contentPanel.add(instructionsPanel, BorderLayout.NORTH);

            // Import Button Panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setOpaque(false);

            JButton importButton = new JButton("Select and Import CSV File");
            importButton.setBackground(HospitalManagementSystem.COLOR_SUCCESS);
            importButton.setForeground(Color.WHITE);
            importButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            importButton.setBorder(new EmptyBorder(15, 30, 15, 30));
            importButton.setFocusPainted(false);
            importButton.addActionListener(e -> selectAndImportCSV());

            buttonPanel.add(importButton);
            contentPanel.add(buttonPanel, BorderLayout.CENTER);

            // Status and Log Panel
            JPanel logPanel = new JPanel(new BorderLayout(5, 5));
            logPanel.setOpaque(false);

            statusLabel = new JLabel("Status: Ready to import");
            statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            logPanel.add(statusLabel, BorderLayout.NORTH);

            logArea = new JTextArea(10, 50);
            logArea.setEditable(false);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
            JScrollPane scrollPane = new JScrollPane(logArea);
            logPanel.add(scrollPane, BorderLayout.CENTER);

            contentPanel.add(logPanel, BorderLayout.SOUTH);

            add(contentPanel, BorderLayout.CENTER);
        }

        private void selectAndImportCSV() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                performImport(filePath);
            }
        }

        private void performImport(String filePath) {
            logArea.setText("");
            statusLabel.setText("Status: Importing...");
            statusLabel.setForeground(new Color(255, 152, 0)); // Orange

            // Run import in background thread to avoid UI freeze
            new Thread(() -> {
                try {
                    logArea.append("Starting CSV import from: " + filePath + "\n");
                    logArea.append("Reading file...\n");

                    HospitalManagementSystem.getDbManager().importDoctorScheduleFromCSV(filePath);

                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: Import Successful!");
                        statusLabel.setForeground(HospitalManagementSystem.COLOR_SUCCESS);
                        logArea.append("✓ Import completed successfully!\n");
                        logArea.append("Doctor schedules have been updated in the database.\n");

                        JOptionPane.showMessageDialog(
                                this,
                                "CSV file imported successfully!\nDoctor schedules updated.",
                                "Import Successful",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    });

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: Import Failed");
                        statusLabel.setForeground(HospitalManagementSystem.COLOR_DANGER);
                        logArea.append("✗ Error: " + e.getMessage() + "\n");

                        JOptionPane.showMessageDialog(
                                this,
                                "Import failed: " + e.getMessage(),
                                "Import Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                    e.printStackTrace();
                }
            }).start();
        }

        private JButton createStyledButton(String text, ActionListener actionListener) {
            JButton button = new JButton(text);
            button.setBackground(HospitalManagementSystem.COLOR_SECONDARY);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBorder(new EmptyBorder(8, 15, 8, 15));
            button.addActionListener(actionListener);
            return button;
        }
    }

    public static class DoctorPage extends JPanel {
        private final Doctor doctor;
        private final JToggleButton dutyToggleButton;

        public DoctorPage(Doctor doctor) {
            this.doctor = doctor;
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            // Top Panel with Welcome Message
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(HospitalManagementSystem.COLOR_PRIMARY);

            JLabel welcomeLabel = new JLabel(" Welcome back, Dr. " + doctor.getName() + "! ");
            welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            welcomeLabel.setForeground(Color.WHITE);
            topPanel.add(welcomeLabel, BorderLayout.WEST);

            // Button Panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            buttonPanel.setOpaque(false);
            buttonPanel.add(createStyledButton("My Patient Details", e -> HospitalManagementSystem.showDoctorSpecificPage("PatientDetailsPageDoctor", doctor)));
            buttonPanel.add(createStyledButton("My Upcoming Appointments", e -> HospitalManagementSystem.showDoctorSpecificPage("AppointmentsViewPageDoctor", doctor)));
            buttonPanel.add(createStyledButton("Start Consultation", e -> showConsultationOptions()));
            topPanel.add(buttonPanel, BorderLayout.CENTER);

            add(topPanel, BorderLayout.NORTH);

            // Center Message
            JLabel centerMessage = new JLabel("Doctor Dashboard", SwingConstants.CENTER);
            centerMessage.setFont(new Font("SansSerif", Font.BOLD, 48));
            centerMessage.setForeground(Color.WHITE);
            add(centerMessage, BorderLayout.CENTER);

            // Bottom Panel with Action Buttons
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            bottomPanel.setOpaque(false);

            // Duty Toggle Button
            dutyToggleButton = new JToggleButton();
            dutyToggleButton.setOpaque(true);
            dutyToggleButton.setContentAreaFilled(true);
            dutyToggleButton.setEnabled(true);
            dutyToggleButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            dutyToggleButton.setBorder(new EmptyBorder(10, 20, 10, 20));
            dutyToggleButton.addActionListener(e -> toggleDutyStatus());
            bottomPanel.add(dutyToggleButton);

            // Logout Button
            JButton logoutButton = createStyledButton("Logout", e -> performLogout());
            bottomPanel.add(logoutButton);

            add(bottomPanel, BorderLayout.SOUTH);

            // Initialize duty status
            boolean isOnDuty = HospitalManagementSystem.getDbManager().isDoctorOnDuty(doctor.getId());
            dutyToggleButton.setSelected(isOnDuty);
            updateDutyButtonText();
            dutyToggleButton.setEnabled(true);
        }

        private void toggleDutyStatus() {
            boolean isOnDuty = dutyToggleButton.isSelected();
            HospitalManagementSystem.getDbManager().updateDoctorDutyStatus(doctor.getId(), isOnDuty);
            updateDutyButtonText();
            dutyToggleButton.setEnabled(true);

            String message = isOnDuty ? "You are now ON DUTY" : "You are now OFF DUTY";
            JOptionPane.showMessageDialog(this, message, "Duty Status Updated", JOptionPane.INFORMATION_MESSAGE);
        }

        private void updateDutyButtonText() {
            if (dutyToggleButton.isSelected()) {
                dutyToggleButton.setText("🟢 ON DUTY");
                dutyToggleButton.setBackground(HospitalManagementSystem.COLOR_SUCCESS);
            } else {
                dutyToggleButton.setText("🔴 OFF DUTY");
                dutyToggleButton.setBackground(HospitalManagementSystem.COLOR_DANGER);
            }
            dutyToggleButton.setForeground(Color.WHITE);
        }

        private void performLogout() {
            HospitalManagementSystem.getDbManager().recordDoctorLogout(doctor.getId());
            HospitalManagementSystem.showPage("LoginPage");
        }

        // In DoctorPage class - modify the showConsultationOptions method
        private void showConsultationOptions() {
            // Check if doctor is on duty first
            boolean isOnDuty = HospitalManagementSystem.getDbManager().isDoctorOnDuty(doctor.getId());
            if (!isOnDuty) {
                JOptionPane.showMessageDialog(this,
                        "You must sign on duty before starting consultations.\nPlease toggle your duty status to ON DUTY first.",
                        "Not On Duty",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<Consultation> upcomingAppointments = HospitalManagementSystem.getDbManager()
                    .getUpcomingAppointments(doctor.getId());

            // FIXED: Filter by date/time AND consultation completion status
            List<Consultation> pendingConsultations = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Consultation appointment : upcomingAppointments) {
                // Check if appointment is today or in the future
                if (appointment.getConsultationDateTime().toLocalDate().isAfter(now.toLocalDate()) ||
                        appointment.getConsultationDateTime().toLocalDate().isEqual(now.toLocalDate())) {

                    // ADDED: Check if consultation is already completed
                    boolean isCompleted = isConsultationCompleted(appointment);

                    // Only add if NOT completed
                    if (!isCompleted) {
                        pendingConsultations.add(appointment);
                    }
                }
            }

            if (pendingConsultations.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No pending appointments available.\nAll scheduled consultations have been completed.",
                        "No Pending Appointments",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] appointmentOptions = new String[pendingConsultations.size()];
            for (int i = 0; i < pendingConsultations.size(); i++) {
                Consultation c = pendingConsultations.get(i);
                Patient p = HospitalManagementSystem.getDbManager().getPatientById(c.getPatientId());

                appointmentOptions[i] = String.format("%s - %s - %s",
                        p.getName(),
                        c.getConsultationDateTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
                        c.getConsultingReason());
            }

            String selected = (String) JOptionPane.showInputDialog(this,
                    "Select appointment to start consultation:",
                    "Start Consultation",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    appointmentOptions,
                    appointmentOptions[0]);

            if (selected != null) {
                int selectedIndex = java.util.Arrays.asList(appointmentOptions).indexOf(selected);
                Consultation selectedConsultation = pendingConsultations.get(selectedIndex);
                Patient selectedPatient = HospitalManagementSystem.getDbManager()
                        .getPatientById(selectedConsultation.getPatientId());

                HospitalManagementSystem.showConsultationPanel(doctor, selectedPatient, selectedConsultation);
            }
        }

        private boolean isConsultationCompleted(Consultation consultation) {
            List<Prescription> prescriptions = HospitalManagementSystem.getDbManager().getPrescriptionsForConsultation(consultation.getId());
            List<MedicalTest> tests = HospitalManagementSystem.getDbManager().getTestsForConsultation(consultation.getId());
            return !prescriptions.isEmpty() || !tests.isEmpty();
        }

        private JButton createStyledButton(String text, ActionListener actionListener) {
            JButton button = new JButton(text);
            button.setBackground(HospitalManagementSystem.COLOR_SECONDARY);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBorder(new EmptyBorder(8, 15, 8, 15));
            button.addActionListener(actionListener);
            return button;
        }
    }

    public static class ConsultationPanel extends JPanel {
        private final Doctor doctor;
        private final Patient patient;
        private final Consultation consultation;
        private JTextArea consultationNotesArea;
        private JPanel prescriptionsPanel, testsPanel;
        private final List<PrescriptionEntry> prescriptionEntries;
        private final List<TestEntry> testEntries;
        private JLabel totalBillLabel;

        public ConsultationPanel(Doctor doctor, Patient patient, Consultation consultation) {
            this.doctor = doctor;
            this.patient = patient;
            this.consultation = consultation;
            this.prescriptionEntries = new ArrayList<>();
            this.testEntries = new ArrayList<>();

            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel topPanel = createPatientInfoPanel();
            contentPanel.add(topPanel, BorderLayout.NORTH);

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("Consultation Notes", createConsultationNotesPanel());
            tabbedPane.addTab("Prescriptions", createPrescriptionsPanel());
            tabbedPane.addTab("Medical Tests", createTestsPanel());
            tabbedPane.addTab("Bill Summary", createBillSummaryPanel());
            contentPanel.add(tabbedPane, BorderLayout.CENTER);

            JPanel bottomPanel = createActionButtonsPanel();
            contentPanel.add(bottomPanel, BorderLayout.SOUTH);

            add(contentPanel, BorderLayout.CENTER);
        }

        private JPanel createPatientInfoPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);
            JLabel title = new JLabel("Consultation - " + patient.getName(), SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            panel.add(title, BorderLayout.CENTER);
            JLabel info = new JLabel("Patient ID: " + patient.getId() + " | Contact: " + patient.getContactNumber(), SwingConstants.CENTER);
            info.setFont(new Font("SansSerif", Font.PLAIN, 14));
            panel.add(info, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel createConsultationNotesPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setOpaque(false);
            JLabel label = new JLabel("Consultation Notes:");
            label.setFont(new Font("SansSerif", Font.BOLD, 14));
            panel.add(label, BorderLayout.NORTH);
            consultationNotesArea = new JTextArea(10, 30);
            consultationNotesArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
            consultationNotesArea.setBorder(new EmptyBorder(10, 10, 10, 10));
            panel.add(new JScrollPane(consultationNotesArea), BorderLayout.CENTER);
            return panel;
        }

        private JPanel createPrescriptionsPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setOpaque(false);
            prescriptionsPanel = new JPanel();
            prescriptionsPanel.setLayout(new BoxLayout(prescriptionsPanel, BoxLayout.Y_AXIS));
            prescriptionsPanel.setOpaque(false);
            JButton addPrescriptionButton = new JButton("+ Add Prescription");
            addPrescriptionButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            addPrescriptionButton.setForeground(Color.WHITE);
            addPrescriptionButton.addActionListener(e -> addPrescriptionEntry());
            panel.add(new JScrollPane(prescriptionsPanel), BorderLayout.CENTER);
            panel.add(addPrescriptionButton, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel createTestsPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setOpaque(false);
            testsPanel = new JPanel();
            testsPanel.setLayout(new BoxLayout(testsPanel, BoxLayout.Y_AXIS));
            testsPanel.setOpaque(false);
            JButton addTestButton = new JButton("+ Add Medical Test");
            addTestButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            addTestButton.setForeground(Color.WHITE);
            addTestButton.addActionListener(e -> addTestEntry());
            panel.add(new JScrollPane(testsPanel), BorderLayout.CENTER);
            panel.add(addTestButton, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel createBillSummaryPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setOpaque(false);
            totalBillLabel = new JLabel();
            totalBillLabel.setVerticalAlignment(SwingConstants.TOP);
            totalBillLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            JButton calculateBillButton = new JButton("Calculate Total Bill");
            calculateBillButton.setBackground(COLOR_SUCCESS);
            calculateBillButton.setForeground(Color.WHITE);
            calculateBillButton.addActionListener(e -> updateBillSummary());
            panel.add(new JScrollPane(totalBillLabel), BorderLayout.CENTER);
            panel.add(calculateBillButton, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel createActionButtonsPanel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            panel.setOpaque(false);
            JButton saveButton = new JButton("Save Consultation");
            saveButton.setBackground(COLOR_SUCCESS);
            saveButton.setForeground(Color.WHITE);
            saveButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            saveButton.addActionListener(e -> saveConsultation());
            JButton backButton = new JButton("Back to Dashboard");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("DoctorPage"));
            panel.add(backButton);
            panel.add(saveButton);
            return panel;
        }

        private void addPrescriptionEntry() {
            PrescriptionEntry entry = new PrescriptionEntry();
            prescriptionEntries.add(entry);
            prescriptionsPanel.add(entry);
            prescriptionsPanel.revalidate();
            prescriptionsPanel.repaint();
        }

        private void addTestEntry() {
            TestEntry entry = new TestEntry();
            testEntries.add(entry);
            testsPanel.add(entry);
            testsPanel.revalidate();
            testsPanel.repaint();
        }

        private void updateBillSummary() {
            StringBuilder sb = new StringBuilder("<html><h3>Consultation Bill Summary</h3><hr>");
            double consultationFee = 500.0, prescriptionTotal = 0.0, testsTotal = 0.0;

            sb.append("<b>Consultation Fee:</b> ₹").append(String.format("%.2f", consultationFee)).append("<br><br>");
            sb.append("<b>Prescribed Medicines:</b><br>");
            for (PrescriptionEntry entry : prescriptionEntries) {
                if (!entry.getMedicineName().trim().isEmpty()) {
                    double medicinePrice = 50.0; // Default price
                    prescriptionTotal += medicinePrice;
                    sb.append("• ").append(entry.getMedicineName()).append(" - ₹").append(String.format("%.2f", medicinePrice)).append("<br>");
                }
            }
            if (prescriptionTotal == 0) sb.append("No medicines prescribed<br>");
            sb.append("<br>");

            sb.append("<b>Medical Tests:</b><br>");
            for (TestEntry entry : testEntries) {
                if (entry.getSelectedTest() != null) {
                    MedicalTest test = entry.getSelectedTest();
                    double testPrice = test.getPrice() * entry.getQuantity();
                    testsTotal += testPrice;
                    sb.append("• ").append(test.getName()).append(" x").append(entry.getQuantity()).append(" - ₹").append(String.format("%.2f", testPrice)).append("<br>");
                }
            }
            if (testsTotal == 0) sb.append("No tests prescribed<br>");

            double subtotal = consultationFee + prescriptionTotal + testsTotal;
            double tax = subtotal * 0.18;
            double total = subtotal + tax;
            sb.append("<br><hr>");
            sb.append("<b>Subtotal:</b> ₹").append(String.format("%.2f", subtotal)).append("<br>");
            sb.append("<b>GST (18%):</b> ₹").append(String.format("%.2f", tax)).append("<br>");
            sb.append("<b>Total Amount:</b> ₹").append(String.format("%.2f", total)).append("<br></html>");
            totalBillLabel.setText(sb.toString());
        }

        private void saveConsultation() {
            for (PrescriptionEntry entry : prescriptionEntries) {
                if (!entry.getMedicineName().trim().isEmpty()) {
                    HospitalManagementSystem.getDbManager().addPrescription(consultation.getId(), entry.getMedicineName(), entry.getDosage());
                }
            }
            for (TestEntry entry : testEntries) {
                if (entry.getSelectedTest() != null) {
                    HospitalManagementSystem.getDbManager().addConsultationTest(consultation.getId(), entry.getSelectedTest().getId(), entry.getQuantity());
                }
            }
            JOptionPane.showMessageDialog(this, "Consultation saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            HospitalManagementSystem.showPage("DoctorPage");
        }

        private class PrescriptionEntry extends JPanel {
            private final JTextField medicineField, dosageField;

            public PrescriptionEntry() {
                setLayout(new FlowLayout(FlowLayout.LEFT));
                setOpaque(false);
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                add(new JLabel("Medicine:"));
                medicineField = new JTextField(20);
                add(medicineField);
                add(new JLabel("Dosage:"));
                dosageField = new JTextField(15);
                add(dosageField);
                JButton removeButton = new JButton("Remove");
                removeButton.setBackground(HospitalManagementSystem.COLOR_DANGER);
                removeButton.setForeground(Color.WHITE);
                removeButton.addActionListener(e -> removePrescription());
                add(removeButton);
            }

            private void removePrescription() {
                prescriptionsPanel.remove(this);
                prescriptionEntries.remove(this);
                prescriptionsPanel.revalidate();
                prescriptionsPanel.repaint();
            }

            public String getMedicineName() {
                return medicineField.getText();
            }

            public String getDosage() {
                return dosageField.getText();
            }
        }

        private class TestEntry extends JPanel {
            private final JComboBox<MedicalTest> testComboBox;
            private final JSpinner quantitySpinner;
            private final JLabel priceLabel;

            public TestEntry() {
                setLayout(new FlowLayout(FlowLayout.LEFT));
                setOpaque(false);
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                add(new JLabel("Test:"));
                List<MedicalTest> tests = HospitalManagementSystem.getDbManager().getAllMedicalTests();
                testComboBox = new JComboBox<>(tests.toArray(new MedicalTest[0]));
                testComboBox.setPreferredSize(new Dimension(300, 25));
                testComboBox.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (value instanceof MedicalTest) {
                            MedicalTest test = (MedicalTest) value;
                            setText(test.getName() + " - ₹" + String.format("%.2f", test.getPrice()));
                        }
                        return this;
                    }
                });
                testComboBox.addActionListener(e -> updatePrice());
                add(testComboBox);
                add(new JLabel("Qty:"));
                quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
                quantitySpinner.setPreferredSize(new Dimension(60, 25));
                quantitySpinner.addChangeListener(e -> updatePrice());
                add(quantitySpinner);
                priceLabel = new JLabel("₹0.00");
                priceLabel.setPreferredSize(new Dimension(100, 25));
                add(priceLabel);
                JButton removeButton = new JButton("Remove");
                removeButton.setBackground(HospitalManagementSystem.COLOR_DANGER);
                removeButton.setForeground(Color.WHITE);
                removeButton.addActionListener(e -> removeTest());
                add(removeButton);
                updatePrice();
            }

            private void updatePrice() {
                if (testComboBox.getSelectedItem() instanceof MedicalTest) {
                    MedicalTest test = (MedicalTest) testComboBox.getSelectedItem();
                    int quantity = (Integer) quantitySpinner.getValue();
                    priceLabel.setText("₹" + String.format("%.2f", test.getPrice() * quantity));
                }
            }

            private void removeTest() {
                testsPanel.remove(this);
                testEntries.remove(this);
                testsPanel.revalidate();
                testsPanel.repaint();
            }

            public MedicalTest getSelectedTest() {
                return (MedicalTest) testComboBox.getSelectedItem();
            }

            public int getQuantity() {
                return (Integer) quantitySpinner.getValue();
            }
        }
    }


    public static class AppointmentBookingPage1 extends JPanel {
        private final JTextField nameField, contactField, ageField;

        public AppointmentBookingPage1() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(255, 255, 255, 230));
            formPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel title = new JLabel("Book an Appointment - Step 1: Patient Info");
            title.setFont(new Font("SansSerif", Font.BOLD, 28));
            gbc.gridwidth = 2;
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(title, gbc);

            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Patient Full Name:"), gbc);
            gbc.gridy = 2;
            formPanel.add(new JLabel("Contact Number:"), gbc);
            gbc.gridy = 3;
            formPanel.add(new JLabel("Age:"), gbc);

            gbc.anchor = GridBagConstraints.WEST;
            nameField = new JTextField(20);
            nameField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridx = 1;
            gbc.gridy = 1;
            formPanel.add(nameField, gbc);

            contactField = new JTextField(20);
            contactField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridy = 2;
            formPanel.add(contactField, gbc);

            ageField = new JTextField(20);
            ageField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gbc.gridy = 3;
            formPanel.add(ageField, gbc);

            JButton nextButton = new JButton("Next");
            nextButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            nextButton.setForeground(Color.WHITE);
            nextButton.addActionListener(e -> proceedToNextStep());
            gbc.gridy = 4;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            formPanel.add(nextButton, gbc);

            JButton backButton = new JButton("Cancel");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("MainPage"));
            gbc.gridy = 5;
            formPanel.add(backButton, gbc);

            add(formPanel, new GridBagConstraints());
        }

        private void proceedToNextStep() {
            String patientName = nameField.getText().trim();
            String contactNumber = contactField.getText().trim();
            String ageText = ageField.getText().trim();

            if (patientName.isEmpty() || contactNumber.isEmpty() || ageText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!patientName.matches("[a-zA-Z ]+")) {
                JOptionPane.showMessageDialog(this, "Patient name should contain only letters and spaces.", "Invalid Name", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!contactNumber.matches("\\d{10}")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid 10-digit phone number (numbers only).", "Invalid Phone Number", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int age = Integer.parseInt(ageText);
                if (age < 0 || age > 150) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid age (0-150).", "Invalid Age", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                HospitalManagementSystem.tempPatientData = new TempPatientData(patientName, contactNumber, age);
                HospitalManagementSystem.startAppointmentBookingWithTempData();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid age (numbers only).", "Invalid Age", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static class AppointmentBookingPage2 extends JPanel {
        private final Patient patient;
        private final JTextArea symptomsArea;

        public AppointmentBookingPage2(Patient patient) {
            this.patient = patient;
            setOpaque(false);
            setLayout(new GridBagLayout());
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(255, 255, 255, 230));
            formPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel title = new JLabel("Step 2: Describe Your Symptoms");
            title.setFont(new Font("SansSerif", Font.BOLD, 28));
            gbc.gridwidth = 2;
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(title, gbc);

            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.NORTHEAST;
            gbc.gridy = 1;
            formPanel.add(new JLabel("Describe Symptoms:"), gbc);
            symptomsArea = new JTextArea(8, 30);
            symptomsArea.setLineWrap(true);
            symptomsArea.setWrapStyleWord(true);
            symptomsArea.setFont(new Font("SansSerif", Font.PLAIN, 16));
            symptomsArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            JScrollPane scrollPane = new JScrollPane(symptomsArea);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            formPanel.add(scrollPane, gbc);

            JButton nextButton = new JButton("Get Doctor Suggestions ➡");
            nextButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            nextButton.setForeground(Color.WHITE);
            nextButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            nextButton.addActionListener(e -> proceedToNextStep());
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.CENTER;
            formPanel.add(nextButton, gbc);

            JButton backButton = new JButton("⬅ Back");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("AppointmentBookingPage1"));
            gbc.gridy = 3;
            formPanel.add(backButton, gbc);

            add(formPanel, new GridBagConstraints());
        }

        private void proceedToNextStep() {
            String symptoms = symptomsArea.getText().trim();
            if (symptoms.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please describe your symptoms.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (patient != null) {
                HospitalManagementSystem.showDoctorSuggestions(patient, symptoms);
            } else {
                HospitalManagementSystem.showDoctorSuggestionsWithTempData(symptoms);
            }
        }
    }

    public static class AppointmentBookingPage25 extends JPanel {
        public AppointmentBookingPage25(Patient patient, String symptoms) {
            setOpaque(false);
            setLayout(new GridBagLayout());

            JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
            mainContainer.setOpaque(true);
            mainContainer.setBackground(new Color(255, 255, 255, 230));
            mainContainer.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel title = new JLabel("Step 3: Recommended Doctors Based on Your Profile", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            mainContainer.add(title, BorderLayout.NORTH);

            // Get patient age for recommendations
            int patientAge = (patient != null) ? patient.getAge() : tempPatientData.age;

            // Get suggested doctors from multiple specializations
            List<Doctor> suggestedDoctors = HospitalManagementSystem.getDbManager()
                    .getSuggestedDoctorsMultiple(symptoms, patientAge);

            // Group doctors by specialization for better display
            Map<String, List<Doctor>> doctorsBySpecialization = groupDoctorsBySpecialization(suggestedDoctors);

            // Create the main panel with sections for each specialization
            JPanel doctorsPanel = new JPanel();
            doctorsPanel.setLayout(new BoxLayout(doctorsPanel, BoxLayout.Y_AXIS));

            // Add recommendation explanation
            JPanel explanationPanel = createExplanationPanel(patientAge, symptoms, doctorsBySpecialization.keySet());
            doctorsPanel.add(explanationPanel);
            doctorsPanel.add(Box.createVerticalStrut(10));

            if (suggestedDoctors.isEmpty()) {
                JLabel noDocLabel = new JLabel("No doctors available. Please contact hospital administration.",
                        SwingConstants.CENTER);
                noDocLabel.setForeground(Color.RED);
                doctorsPanel.add(noDocLabel);
            } else {
                // Display doctors grouped by specialization
                for (Map.Entry<String, List<Doctor>> entry : doctorsBySpecialization.entrySet()) {
                    String specialization = entry.getKey();
                    List<Doctor> doctors = entry.getValue();

                    // Add section header for each specialization
                    JPanel sectionPanel = createSpecializationSection(specialization, doctors, patient, symptoms);
                    doctorsPanel.add(sectionPanel);
                    doctorsPanel.add(Box.createVerticalStrut(15));
                }
            }

            JScrollPane scrollPane = new JScrollPane(doctorsPanel);
            scrollPane.setPreferredSize(new Dimension(700, 400));
            mainContainer.add(scrollPane, BorderLayout.CENTER);

            // Back button
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton backButton = new JButton("Back to Symptoms");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("AppointmentBookingPage2"));
            buttonPanel.add(backButton);

            mainContainer.add(buttonPanel, BorderLayout.SOUTH);
            add(mainContainer, new GridBagConstraints());
        }

        // Helper method to group doctors by specialization
        private Map<String, List<Doctor>> groupDoctorsBySpecialization(List<Doctor> doctors) {
            Map<String, List<Doctor>> grouped = new HashMap<>();

            for (Doctor doctor : doctors) {
                String specialization = doctor.getSpecialization();
                grouped.computeIfAbsent(specialization, k -> new ArrayList<>()).add(doctor);
            }

            return grouped;
        }

        // Create explanation panel showing why these doctors are recommended
        private JPanel createExplanationPanel(int patientAge, String symptoms, Set<String> specializations) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Recommendation Logic"));
            panel.setBackground(new Color(240, 248, 255));

            StringBuilder explanation = new StringBuilder("<html>");
            explanation.append("<h3>Why These Doctors Are Recommended:</h3>");

            // Age-based recommendation
            if (patientAge < 18) {
                explanation.append("<p><b>👶 Age Factor:</b> Patient is ").append(patientAge)
                        .append(" years old, so <b>Pediatrics</b> is recommended for specialized child care.</p>");
            }

            // Symptom-based recommendations
            symptoms = symptoms.toLowerCase();
            if (symptoms.contains("heart") || symptoms.contains("chest") || symptoms.contains("cardiac")) {
                explanation.append("<p><b>❤️ Symptoms Factor:</b> Heart/chest-related symptoms detected, so <b>Cardiology</b> is recommended for specialized cardiac evaluation.</p>");
            }
            if (symptoms.contains("bone") || symptoms.contains("joint") || symptoms.contains("fracture")) {
                explanation.append("<p><b>🦴 Symptoms Factor:</b> Bone/joint-related symptoms detected, so <b>Orthopedics</b> is recommended.</p>");
            }

            // Combined recommendation logic
            if (patientAge < 18 && (symptoms.contains("heart") || symptoms.contains("chest"))) {
                explanation.append("<div style='background-color: #e8f5e8; padding: 10px; border-radius: 5px; margin: 10px 0;'>");
                explanation.append("<b>⚕️ Multiple Specialist Approach:</b> Since this is a child with heart-related symptoms, ");
                explanation.append("we recommend consulting BOTH a Pediatrician (for age-appropriate care) AND a Cardiologist ");
                explanation.append("(for heart-specific evaluation). This ensures comprehensive care.");
                explanation.append("</div>");
            }

            explanation.append("<p><i>You can choose any doctor from the recommended specializations below.</i></p>");
            explanation.append("</html>");

            JLabel explanationLabel = new JLabel(explanation.toString());
            explanationLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(explanationLabel, BorderLayout.CENTER);

            return panel;
        }

        // Create a section for each specialization
        private JPanel createSpecializationSection(String specialization, List<Doctor> doctors,
                                                   Patient patient, String symptoms) {
            JPanel sectionPanel = new JPanel(new BorderLayout());
            sectionPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(getSpecializationColor(specialization), 2),
                    specialization + " (" + doctors.size() + " available)",
                    TitledBorder.LEFT, TitledBorder.TOP,
                    new Font("SansSerif", Font.BOLD, 14),
                    getSpecializationColor(specialization)
            ));
            sectionPanel.setBackground(Color.WHITE);

            JPanel doctorsGrid = new JPanel(new GridLayout(0, 1, 5, 5));

            for (Doctor doctor : doctors) {
                JPanel doctorPanel = new JPanel(new BorderLayout(10, 5));
                doctorPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                doctorPanel.setBackground(Color.WHITE);
                doctorPanel.setPreferredSize(new Dimension(600, 60));

                // Doctor information with specialization-specific icon
                String icon = getSpecializationIcon(specialization);
                JLabel doctorInfo = new JLabel("<html>" + icon + " <b>Dr. " + doctor.getName() + "</b><br>" +
                        "<span style='color: #666; font-size: 12px;'>Specialization: " + doctor.getSpecialization() + "</span><br>" +
                        "<span style='color: " + (doctor.isOnDuty() ? "green" : "red") + "; font-size: 11px;'>" +
                        (doctor.isOnDuty() ? "✓ Available Now" : "✗ Currently Off Duty") + "</span></html>");
                doctorInfo.setFont(new Font("SansSerif", Font.PLAIN, 13));
                doctorPanel.add(doctorInfo, BorderLayout.CENTER);

                JButton selectButton = new JButton("Select");
                selectButton.setBackground(getSpecializationColor(specialization));
                selectButton.setForeground(Color.WHITE);
                selectButton.setFont(new Font("SansSerif", Font.BOLD, 12));
                selectButton.addActionListener(e -> {
                    if (patient != null) {
                        HospitalManagementSystem.showDoctorCalendar(patient, doctor, symptoms);
                    } else {
                        HospitalManagementSystem.showDoctorCalendarWithTempData(doctor, symptoms);
                    }
                });
                doctorPanel.add(selectButton, BorderLayout.EAST);

                doctorsGrid.add(doctorPanel);
            }

            sectionPanel.add(doctorsGrid, BorderLayout.CENTER);
            return sectionPanel;
        }

        // Helper methods for UI styling
        private Color getSpecializationColor(String specialization) {
            switch (specialization.toLowerCase()) {
                case "pediatrics": return new Color(255, 152, 0); // Orange
                case "cardiology": return new Color(244, 67, 54); // Red
                case "orthopedics": return new Color(96, 125, 139); // Blue Grey
                case "dermatology": return new Color(156, 39, 176); // Purple
                case "ophthalmology": return new Color(0, 150, 136); // Teal
                default: return new Color(76, 175, 80); // Green
            }
        }

        private String getSpecializationIcon(String specialization) {
            switch (specialization.toLowerCase()) {
                case "pediatrics": return "👶";
                case "cardiology": return "❤️";
                case "orthopedics": return "🦴";
                case "dermatology": return "🧴";
                case "ophthalmology": return "👁️";
                default: return "⚕️";
            }
        }
    }



    public static class AppointmentBookingPage3 extends JPanel {
        public AppointmentBookingPage3(Patient patient, Doctor doctor, String reason) {
            setOpaque(false);
            setLayout(new GridBagLayout());
            JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
            mainContainer.setOpaque(true);
            mainContainer.setBackground(new Color(255, 255, 255, 230));
            mainContainer.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel title = new JLabel("Step 4: Select Date for Dr. " + doctor.getName(), SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            mainContainer.add(title, BorderLayout.NORTH);

            DoctorCalendarPanel calendarPanel = new DoctorCalendarPanel(doctor, date -> {HospitalManagementSystem.showTimeSlots(patient, doctor, reason, date);
            });
            mainContainer.add(calendarPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            bottomPanel.setOpaque(false);
            JButton backButton = new JButton("⬅ Back to Doctor Selection");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showDoctorSuggestions(patient, reason));
            bottomPanel.add(backButton);
            mainContainer.add(bottomPanel, BorderLayout.SOUTH);

            add(mainContainer, new GridBagConstraints());
        }
    }

    public static class AppointmentBookingPage4 extends JPanel {
        public AppointmentBookingPage4(Patient patient, Doctor doctor, String reason, LocalDate date) {
            setOpaque(false);
            setLayout(new GridBagLayout());
            JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
            mainContainer.setOpaque(true);
            mainContainer.setBackground(new Color(255, 255, 255, 230));
            mainContainer.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel title = new JLabel("Step 5: Select Time on " + date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            mainContainer.add(title, BorderLayout.NORTH);

            TimeSlotSelectionPanel timePanel = new TimeSlotSelectionPanel(doctor, date, time -> {
                String patientName = (patient != null) ? patient.getName() : tempPatientData.name;
                int choice = JOptionPane.showConfirmDialog(this,
                        "Confirm appointment for " + patientName + " with Dr. " + doctor.getName() + " on " + date + " at " + time.format(DateTimeFormatter.ofPattern("HH:mm")) + "?",
                        "Confirm Booking", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    createPatientAndAppointment(doctor, reason, LocalDateTime.of(date, time));
                }
            });
            mainContainer.add(timePanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            bottomPanel.setOpaque(false);
            JButton backButton = new JButton("⬅ Back to Calendar");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showDoctorCalendar(patient, doctor, reason));
            bottomPanel.add(backButton);
            mainContainer.add(bottomPanel, BorderLayout.SOUTH);

            add(mainContainer, new GridBagConstraints());
        }

        private void createPatientAndAppointment(Doctor doctor, String reason, LocalDateTime dateTime) {
            try {
                Patient newPatient = new Patient(0, tempPatientData.name, tempPatientData.contactNumber, tempPatientData.age);

                int generatedPatientId = HospitalManagementSystem.getDbManager().addPatient(newPatient);

                if (generatedPatientId > 0) {
                    newPatient.setId(generatedPatientId);

                    HospitalManagementSystem.getDbManager().recordConsultation(
                            generatedPatientId,
                            doctor.getId(),
                            reason,
                            dateTime,
                            null
                    );

                    String confirmationMessage = String.format(
                            "Booking confirmed successfully!\n\n" +
                                    "Patient Details:\n" +
                                    "• Name: %s\n" +
                                    "• Patient ID: %d\n" +
                                    "• Contact: %s\n\n" +
                                    "Appointment Details:\n" +
                                    "• Doctor: Dr. %s\n" +
                                    "• Date & Time: %s\n" +
                                    "• Reason: %s\n\n" +
                                    "Please save your Patient ID: %d for future reference.",
                            tempPatientData.name,
                            generatedPatientId,
                            tempPatientData.contactNumber,
                            doctor.getName(),
                            dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                            reason,
                            generatedPatientId
                    );

                    JOptionPane.showMessageDialog(this, confirmationMessage,
                            "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);

                    HospitalManagementSystem.tempPatientData = null;

                    // FIXED: Smart redirect based on MAC address
                    redirectToAppropriateMainPage();

                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to create patient record. Please try again.",
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "An error occurred: " + e.getMessage(),
                        "System Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Add this helper method to AppointmentBookingPage4 class
        private void redirectToAppropriateMainPage() {
            try {
                String macAddress = HospitalManagementSystem.getMacAddress();
                InetAddress address = InetAddress.getLocalHost();
                String ipAddress = address.getHostAddress();

                // Check if on hospital network
                if (HospitalManagementSystem.isHospitalIP(ipAddress)) {
                    // On hospital network - check MAC address
                    if (HospitalManagementSystem.isAdminMacAddress(macAddress)) {
                        // Admin workstation - go to MainPage (which routes to admin)
                        HospitalManagementSystem.showPage("MainPage");
                    } else {
                        // Hospital staff - go to HospitalMainPage
                        HospitalManagementSystem.showPage("HospitalMainPage");
                    }
                } else {
                    // External network - go to PublicMainPage
                    HospitalManagementSystem.showPage("PublicMainPage");
                }
            } catch (Exception e) {
                // On error, default to public page
                HospitalManagementSystem.showPage("PublicMainPage");
            }
        }
    }
    public static class PatientInfoPage extends JPanel {
        public PatientInfoPage(Patient patient) {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));
            tabbedPane.setBackground(Color.WHITE);
            tabbedPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
            JLabel title = new JLabel("Patient Portal - " + patient.getName(), SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            topPanel.add(title, BorderLayout.CENTER);
            JButton backButton = new JButton("⬅ Back to Login");
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("PatientLoginPage"));
            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            backButtonPanel.setOpaque(false);
            backButtonPanel.add(backButton);
            topPanel.add(backButtonPanel, BorderLayout.EAST);
            contentPanel.add(topPanel, BorderLayout.NORTH);
            tabbedPane.addTab("Consultation History", createConsultationsPanel(patient));
            tabbedPane.addTab("Prescriptions", createPrescriptionsPanel(patient));
            contentPanel.add(tabbedPane, BorderLayout.CENTER);
            add(contentPanel, BorderLayout.CENTER);
        }

        private JScrollPane createConsultationsPanel(Patient patient) {
            String[] columnNames = {"Date & Time", "Doctor", "Reason", "Next Appointment"};
            DefaultTableModel consultationModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable consultationsTable = new JTable(consultationModel);

            // Apply proper table styling
            consultationsTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
            consultationsTable.setRowHeight(25);
            consultationsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
            consultationsTable.getTableHeader().setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            consultationsTable.getTableHeader().setForeground(Color.WHITE);
            consultationsTable.setGridColor(new Color(180, 180, 180));
            consultationsTable.setShowGrid(true);
            consultationsTable.setSelectionBackground(new Color(232, 245, 233));

            // Set solid border instead of dotted
            consultationsTable.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

            // Load consultation data
            List<Consultation> consultations = HospitalManagementSystem.getDbManager().getConsultationsForPatient(patient.getId());

            if (consultations.isEmpty()) {
                consultationModel.addRow(new Object[]{"No consultation history found", "", "", ""});
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (Consultation c : consultations) {
                    Doctor doctor = HospitalManagementSystem.getDbManager().getDoctorById(c.getDoctorId());
                    String doctorName = doctor != null ? doctor.getName() : "N/A";
                    String nextDate = c.getNextConsultingDate() != null ? c.getNextConsultingDate().toString() : "N/A";

                    consultationModel.addRow(new Object[]{
                            c.getConsultationDateTime().format(formatter),
                            doctorName,
                            c.getConsultingReason(),
                            nextDate
                    });
                }
            }

            JScrollPane scrollPane = new JScrollPane(consultationsTable);
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2)); // Solid border
            return scrollPane;
        }

        private JScrollPane createPrescriptionsPanel(Patient patient) {
            // Create table with proper styling
            String[] columnNames = {"Date", "Doctor", "Medicine", "Dosage"};
            DefaultTableModel prescriptionModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable prescriptionsTable = new JTable(prescriptionModel);

            // Apply proper table styling
            prescriptionsTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
            prescriptionsTable.setRowHeight(25);
            prescriptionsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
            prescriptionsTable.getTableHeader().setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            prescriptionsTable.getTableHeader().setForeground(Color.WHITE);
            prescriptionsTable.setGridColor(new Color(180, 180, 180));
            prescriptionsTable.setShowGrid(true);
            prescriptionsTable.setSelectionBackground(new Color(232, 245, 233));
            prescriptionsTable.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

            // FIXED: Load ALL prescriptions for this patient across all consultations
            List<Consultation> consultations = HospitalManagementSystem.getDbManager()
                    .getConsultationsForPatient(patient.getId());

            boolean hasPrescriptions = false;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Debug: Check if consultations exist
            System.out.println("DEBUG: Found " + consultations.size() + " consultations for patient " + patient.getId());

            for (Consultation c : consultations) {
                List<Prescription> prescriptions = HospitalManagementSystem.getDbManager()
                        .getPrescriptionsForConsultation(c.getId());

                // Debug: Check prescriptions for each consultation
                System.out.println("DEBUG: Consultation " + c.getId() + " has " + prescriptions.size() + " prescriptions");

                if (!prescriptions.isEmpty()) {
                    hasPrescriptions = true;
                    Doctor doctor = HospitalManagementSystem.getDbManager().getDoctorById(c.getDoctorId());
                    String doctorName = (doctor != null) ? doctor.getName() : "N/A";
                    String date = c.getConsultationDateTime().format(dateFormatter);

                    // Add each prescription as a row
                    for (Prescription p : prescriptions) {
                        prescriptionModel.addRow(new Object[]{
                                date,
                                doctorName,
                                p.getMedicineName(),
                                p.getDosage()
                        });
                    }
                }
            }

            // If no prescriptions found, show message
            if (!hasPrescriptions) {
                prescriptionModel.addRow(new Object[]{
                        "No prescriptions found", "", "", ""
                });
            }

            JScrollPane scrollPane = new JScrollPane(prescriptionsTable);
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

            return scrollPane;
        }
    }

        public static class PatientDetailsPage extends JPanel {
        private JTable patientsTable;
        private DefaultTableModel patientsTableModel;
        private final JTextField searchField;
        private final boolean isAdminView;
        private final Doctor doctor;

        public PatientDetailsPage(boolean isAdminView, Doctor doctor) {
            this.isAdminView = isAdminView;
            this.doctor = doctor;

            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);

            JLabel title = new JLabel(isAdminView ? "All Patient Details" : "My Patient Details",
                    SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            topPanel.add(title, BorderLayout.NORTH);

            // Control panel with search functionality
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controlPanel.setOpaque(false);

            if (isAdminView) {
                controlPanel.add(new JLabel("Search by Patient Name:"));
                searchField = new JTextField(20);
                searchField.addActionListener(e -> searchPatients());
                controlPanel.add(searchField);

                JButton searchButton = new JButton("Search");
                searchButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
                searchButton.setForeground(Color.WHITE);
                searchButton.addActionListener(e -> searchPatients());
                controlPanel.add(searchButton);

                JButton clearButton = new JButton("Show All");
                clearButton.setBackground(Color.LIGHT_GRAY);
                clearButton.addActionListener(e -> {
                    searchField.setText("");
                    loadPatientData();
                });
                controlPanel.add(clearButton);
            } else {
                searchField = null;
            }

            JButton backButton = new JButton("⬅ Back to Dashboard");
            backButton.addActionListener(e -> HospitalManagementSystem.showPage(isAdminView ? "AdminPage" : "DoctorPage"));
            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            backButtonPanel.setOpaque(false);
            backButtonPanel.add(backButton);

            topPanel.add(controlPanel, BorderLayout.CENTER);
            topPanel.add(backButtonPanel, BorderLayout.EAST);
            contentPanel.add(topPanel, BorderLayout.NORTH);

            // Create patients table
            String[] columnNames = {"Patient ID", "Patient Name", "Contact Number", "Age", "Total Consultations"};
            patientsTableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            patientsTable = new JTable(patientsTableModel);
            patientsTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
            patientsTable.setRowHeight(25);
            patientsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
            patientsTable.getTableHeader().setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            patientsTable.getTableHeader().setForeground(Color.WHITE);
            patientsTable.setGridColor(new Color(230, 230, 230));
            patientsTable.setShowGrid(true);
            patientsTable.setSelectionBackground(new Color(232, 245, 233));

            // Set column widths
            patientsTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // ID
            patientsTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Name
            patientsTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Contact
            patientsTable.getColumnModel().getColumn(3).setPreferredWidth(60);   // Age
            patientsTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // Consultations

            // Style alternating rows
            patientsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                    }
                    return c;
                }
            });

            // Enable table sorting
            patientsTable.setAutoCreateRowSorter(true);

            // Add double-click functionality for patient details
            patientsTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = patientsTable.getSelectedRow();
                        if (row >= 0) {
                            String patientId = patientsTable.getValueAt(row, 0).toString();
                            String patientName = patientsTable.getValueAt(row, 1).toString();
                            String contact = patientsTable.getValueAt(row, 2).toString();
                            String age = patientsTable.getValueAt(row, 3).toString();

                            JOptionPane.showMessageDialog(PatientDetailsPage.this,
                                    String.format("Patient Information:\nID: %s\nName: %s\nContact: %s\nAge: %s",
                                            patientId, patientName, contact, age),
                                    "Patient Details", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(patientsTable);
            scrollPane.getViewport().setBackground(Color.WHITE);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            add(contentPanel, BorderLayout.CENTER);

            loadPatientData();
        }

        // In PatientDetailsPage - modify the loadPatientData method
        private void loadPatientData() {
            List<Patient> patients;
            if (isAdminView) {
                patients = HospitalManagementSystem.getDbManager().getAllPatients();
            } else {
                patients = HospitalManagementSystem.getDbManager().getPatientsByDoctor(doctor.getId());
            }
            displayPatients(patients);
        }

        private void searchPatients() {
            if (searchField != null) {
                String searchTerm = searchField.getText().trim();
                if (searchTerm.isEmpty()) {
                    loadPatientData();
                } else {
                    List<Patient> patients = HospitalManagementSystem.getDbManager().getPatientsByName(searchTerm);
                    displayPatients(patients);
                }
            }
        }

        private void displayPatients(List<Patient> patients) {
            patientsTableModel.setRowCount(0);

            if (patients.isEmpty()) {
                patientsTableModel.addRow(new Object[]{"", "No patient records found", "", "", ""});
            } else {
                for (Patient p : patients) {
                    // Get consultation count for this patient
                    List<Consultation> consultations = HospitalManagementSystem.getDbManager().getConsultationsForPatient(p.getId());
                    patientsTableModel.addRow(new Object[]{
                            p.getId(),
                            p.getName(),
                            p.getContactNumber(),
                            p.getAge(),
                            consultations.size()
                    });
                }
            }
        }
    }


    public static class DoctorStatusPage extends JPanel {
        private JTable doctorStatusTable;
        private DefaultTableModel tableModel;
        private Timer refreshTimer;

        public DoctorStatusPage() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Top panel with title and back button
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);

            JLabel title = new JLabel("Real-Time Doctor Status", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            topPanel.add(title, BorderLayout.NORTH);

            JButton backButton = new JButton("⬅ Back to Dashboard");
            backButton.addActionListener(e -> {
                if (refreshTimer != null) refreshTimer.stop();
                HospitalManagementSystem.showPage("AdminPage");
            });
            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            backButtonPanel.setOpaque(false);
            backButtonPanel.add(backButton);
            topPanel.add(backButtonPanel, BorderLayout.EAST);

            contentPanel.add(topPanel, BorderLayout.NORTH);

            // Create table with proper columns
            String[] columnNames = {"Doctor Name", "Specialty", "Login Status", "Duty Status", "Last Activity"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Make table read-only
                }
            };

            doctorStatusTable = new JTable(tableModel);
            doctorStatusTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
            doctorStatusTable.setRowHeight(30);
            doctorStatusTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
            doctorStatusTable.getTableHeader().setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            doctorStatusTable.getTableHeader().setForeground(Color.WHITE);
            doctorStatusTable.setGridColor(new Color(230, 230, 230));
            doctorStatusTable.setShowGrid(true);
            doctorStatusTable.setSelectionBackground(new Color(232, 245, 233));

            // Set column widths
            doctorStatusTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Doctor Name
            doctorStatusTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Specialty
            doctorStatusTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Login Status
            doctorStatusTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Duty Status
            doctorStatusTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Last Activity

            // Style alternating rows
            doctorStatusTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                    }

                    // Color code status columns
                    if (column == 2) { // Login Status
                        if (value.toString().contains("Logged In")) {
                            setForeground(HospitalManagementSystem.COLOR_SUCCESS);
                        } else {
                            setForeground(HospitalManagementSystem.COLOR_DANGER);
                        }
                    } else if (column == 3) { // Duty Status
                        if (value.toString().contains("ON DUTY")) {
                            setForeground(HospitalManagementSystem.COLOR_SUCCESS);
                        } else {
                            setForeground(HospitalManagementSystem.COLOR_DANGER);
                        }
                    } else {
                        setForeground(HospitalManagementSystem.COLOR_FONT_DARK);
                    }

                    return c;
                }
            });

            JScrollPane scrollPane = new JScrollPane(doctorStatusTable);
            scrollPane.getViewport().setBackground(Color.WHITE);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            add(contentPanel, BorderLayout.CENTER);

            // Auto-refresh timer
            refreshTimer = new Timer(5000, e -> updateStatus());
            refreshTimer.setInitialDelay(0);
            refreshTimer.start();

            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
                    if (refreshTimer != null) refreshTimer.stop();
                }
            });
        }

        private void updateStatus() {
            List<Doctor> doctors = HospitalManagementSystem.getDbManager().getAllDoctors();
            tableModel.setRowCount(0); // Clear existing rows

            if (doctors.isEmpty()) {
                tableModel.addRow(new Object[]{"No doctors found", "", "", "", ""});
                return;
            }

            for (Doctor d : doctors) {
                String[] loginInfo = HospitalManagementSystem.getDbManager().getDoctorCurrentLoginStatus(d.getId());
                String dutyStatus = HospitalManagementSystem.getDbManager().isDoctorOnDuty(d.getId()) ? "🟢 ON DUTY" : "🔴 OFF DUTY";
                tableModel.addRow(new Object[]{
                        d.getName(),
                        d.getSpecialization(),
                        loginInfo[0],
                        dutyStatus,
                        loginInfo[1]
                });
            }
        }
    }


    public static class AppointmentsViewPage extends JPanel {
        private JTable appointmentsTable;
        private DefaultTableModel appointmentsTableModel;
        private final boolean isAdminView;
        private final Doctor doctor;

        public AppointmentsViewPage(boolean isAdminView, Doctor doctor) {
            this.isAdminView = isAdminView;
            this.doctor = doctor;

            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Top panel
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);

            JLabel title = new JLabel(isAdminView ? "All Upcoming Appointments" : "My Upcoming Appointments",
                    SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            topPanel.add(title, BorderLayout.CENTER);

            JButton backButton = new JButton("⬅ Back to Dashboard");
            backButton.addActionListener(e -> HospitalManagementSystem.showPage(isAdminView ? "AdminPage" : "DoctorPage"));
            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            backButtonPanel.setOpaque(false);
            backButtonPanel.add(backButton);
            topPanel.add(backButtonPanel, BorderLayout.EAST);

            contentPanel.add(topPanel, BorderLayout.NORTH);

            // Create appointments table
            String[] columnNames = {"Patient ID", "Patient Name", "Doctor Name", "Appointment Date", "Appointment Time", "Reason"};
            appointmentsTableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            appointmentsTable = new JTable(appointmentsTableModel);
            appointmentsTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
            appointmentsTable.setRowHeight(25);
            appointmentsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
            appointmentsTable.getTableHeader().setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            appointmentsTable.getTableHeader().setForeground(Color.WHITE);
            appointmentsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            appointmentsTable.setGridColor(new Color(230, 230, 230));
            appointmentsTable.setShowGrid(true);
            appointmentsTable.setSelectionBackground(new Color(232, 245, 233));

            // Set column widths
            appointmentsTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Patient ID
            appointmentsTable.getColumnModel().getColumn(1).setPreferredWidth(150);  // Patient Name
            appointmentsTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Doctor Name
            appointmentsTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // Date
            appointmentsTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Time
            appointmentsTable.getColumnModel().getColumn(5).setPreferredWidth(200);  // Reason

            // Style alternating rows
            appointmentsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                    }
                    return c;
                }
            });

            // Enable table sorting
            appointmentsTable.setAutoCreateRowSorter(true);

            // Add double-click functionality
            appointmentsTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = appointmentsTable.getSelectedRow();
                        if (row >= 0) {
                            String patientName = (String) appointmentsTableModel.getValueAt(row, 1);
                            String doctorName = (String) appointmentsTableModel.getValueAt(row, 2);
                            String date = (String) appointmentsTableModel.getValueAt(row, 3);
                            String time = (String) appointmentsTableModel.getValueAt(row, 4);
                            JOptionPane.showMessageDialog(AppointmentsViewPage.this,
                                    String.format("Appointment Details:\nPatient: %s\nDoctor: %s\nDate: %s\nTime: %s",
                                            patientName, doctorName, date, time),
                                    "Appointment Details", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(appointmentsTable);
            scrollPane.getViewport().setBackground(Color.WHITE);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            add(contentPanel, BorderLayout.CENTER);

            loadAppointmentData();
        }

        private void loadAppointmentData() {
            List<Consultation> appointments;

            if (isAdminView) {
                appointments = HospitalManagementSystem.getDbManager().getAllUpcomingAppointments();
            } else {
                appointments = HospitalManagementSystem.getDbManager().getUpcomingAppointments(doctor.getId());
            }

            appointmentsTableModel.setRowCount(0);

            if (appointments.isEmpty()) {
                appointmentsTableModel.addRow(new Object[]{"", "No upcoming appointments found", "", "", "", ""});
            } else {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                LocalDateTime now = LocalDateTime.now();

                for (Consultation a : appointments) {
                    // Only show appointments that are today or in the future
                    if (a.getConsultationDateTime().toLocalDate().isAfter(now.toLocalDate()) ||
                            a.getConsultationDateTime().toLocalDate().isEqual(now.toLocalDate())) {

                        // Check if consultation is completed
                        List<Prescription> prescriptions = HospitalManagementSystem.getDbManager()
                                .getPrescriptionsForConsultation(a.getId());
                        List<MedicalTest> tests = HospitalManagementSystem.getDbManager()
                                .getTestsForConsultation(a.getId());

                        // FIXED: Skip this appointment if already consulted (has prescriptions or tests)
                        boolean isConsulted = (!prescriptions.isEmpty() || !tests.isEmpty());

                        if (!isConsulted) {
                            // Only add appointments that haven't been consulted yet
                            Patient p = HospitalManagementSystem.getDbManager().getPatientById(a.getPatientId());
                            Doctor d = HospitalManagementSystem.getDbManager().getDoctorById(a.getDoctorId());

                            appointmentsTableModel.addRow(new Object[]{
                                    p != null ? p.getId() : -1,
                                    p != null ? p.getName() : "N/A",
                                    d != null ? "Dr. " + d.getName() : "N/A",
                                    a.getConsultationDateTime().format(dateFormatter),
                                    a.getConsultationDateTime().format(timeFormatter),
                                    a.getConsultingReason()
                            });
                        }
                    }
                }

                // If no unconsulted appointments found, show message
                if (appointmentsTableModel.getRowCount() == 0) {
                    appointmentsTableModel.addRow(new Object[]{"", "All appointments have been consulted", "", "", "", ""});
                }
            }
        }
    }

        public static class UserManagementPage extends JPanel {
        public UserManagementPage() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel title = new JLabel("User Management", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            contentPanel.add(title, BorderLayout.NORTH);
            JLabel centerMessage = new JLabel("User Management Features Coming Soon", SwingConstants.CENTER);
            centerMessage.setFont(new Font("SansSerif", Font.BOLD, 18));
            contentPanel.add(centerMessage, BorderLayout.CENTER);

            add(contentPanel, BorderLayout.CENTER);
        }
    }

    public static class OnlinePharmacyPage extends JPanel {

        public OnlinePharmacyPage() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            // Main content panel - make it more opaque
            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 240)); // More opaque
            contentPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

            // Title
            JLabel title = new JLabel("Online Pharmacy", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(HospitalManagementSystem.COLOR_FONT_DARK);
            contentPanel.add(title, BorderLayout.NORTH);

            // Center panel with options - simplified layout
            JPanel optionsPanel = new JPanel(new GridLayout(2, 1, 20, 20));
            optionsPanel.setOpaque(true); // Make it opaque
            optionsPanel.setBackground(new Color(255, 255, 255, 200));
            optionsPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

            // Patient Medicine Button
            JButton patientMedicineButton = createStyledButton("Get Patient Medicines",
                    "Access prescribed medicines for specific patients");
            patientMedicineButton.addActionListener(e -> HospitalManagementSystem.showPage("PatientMedicinePage"));

            // Pharmacy Stock Button
            JButton pharmacyStockButton = createStyledButton("View Pharmacy Stock",
                    "Browse all available medicines in pharmacy");
            pharmacyStockButton.addActionListener(e -> HospitalManagementSystem.showPage("PharmacyStockPage"));

            optionsPanel.add(patientMedicineButton);
            optionsPanel.add(pharmacyStockButton);

            contentPanel.add(optionsPanel, BorderLayout.CENTER);

            // Back button
            JButton backButton = new JButton("⬅ Back to Home");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            backButton.setBorder(new EmptyBorder(10, 20, 10, 20));
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("MainPage"));

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bottomPanel.setOpaque(false);
            bottomPanel.add(backButton);
            contentPanel.add(bottomPanel, BorderLayout.SOUTH);

            add(contentPanel, BorderLayout.CENTER);
        }

        private JButton createStyledButton(String text, String description) {
            // Create a simple, visible button instead of complex panel
            JButton button = new JButton();
            button.setLayout(new BorderLayout());

            // Main text
            JLabel titleLabel = new JLabel(text, SwingConstants.CENTER);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);

            // Description text
            JLabel descLabel = new JLabel("<html><center>" + description + "</center></html>", SwingConstants.CENTER);
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            descLabel.setForeground(new Color(220, 220, 220));

            // Add labels to button
            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.setOpaque(false);
            textPanel.add(titleLabel, BorderLayout.CENTER);
            textPanel.add(descLabel, BorderLayout.SOUTH);

            button.add(textPanel, BorderLayout.CENTER);

            // Style the button
            button.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            button.setForeground(Color.WHITE);
            button.setBorder(new EmptyBorder(20, 20, 20, 20));
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setPreferredSize(new Dimension(400, 100));

            // Add hover effect
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(HospitalManagementSystem.COLOR_SECONDARY);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
                }
            });

            return button;
        }
    }

    public static class PatientMedicinePage extends JPanel {
        private final JTextField searchField;
        private final JTextField patientIdField;
        private final JPanel resultsPanel;
        private final Map<Medicine, Integer> cart = new HashMap<>();
        private final JLabel cartLabel;
        private List<Medicine> cartItems = new ArrayList<>();

        public PatientMedicinePage() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            // Top panel for patient ID, search and cart
            JPanel topPanel = new JPanel(new BorderLayout(10, 10));
            topPanel.setOpaque(false);

            // Patient ID panel (top section)
            JPanel patientIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            patientIdPanel.setOpaque(false);
            patientIdField = new JTextField(15);
            JButton loadButton = new JButton("Load Patient Medicines");
            loadButton.addActionListener(e -> {
                String patientIdStr = patientIdField.getText().trim();

                if (patientIdStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a Patient ID",
                            "Input Required",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    int patientId = Integer.parseInt(patientIdStr);

                    // Call the method to get prescriptions
                    List<Prescription> prescriptions = HospitalManagementSystem.getDbManager()
                            .getAllPrescriptionsForPatient(patientId);

                    if (prescriptions.isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "No prescribed medicines found for this patient.",
                                "No Results",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // Display prescriptions (create UI to show them)
                        displayPrescriptions(prescriptions, patientId);
                    }

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a valid numeric Patient ID",
                            "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

            patientIdPanel.add(new JLabel("Patient ID:"));
            patientIdPanel.add(patientIdField);
            patientIdPanel.add(loadButton);  // CORRECT

            // Search components (middle section)
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.setOpaque(false);
            searchField = new JTextField(30);
            JButton searchButton = new JButton("Search");
            searchPanel.add(new JLabel("Search Medicines:"));
            searchPanel.add(searchField);
            searchPanel.add(searchButton);

            // Combine patient ID and search panels
            JPanel leftControlsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            leftControlsPanel.setOpaque(false);
            leftControlsPanel.add(patientIdPanel);
            leftControlsPanel.add(searchPanel);

            topPanel.add(leftControlsPanel, BorderLayout.CENTER);

            // Cart components (right section)
            JPanel cartPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            cartPanel.setOpaque(false);
            cartLabel = new JLabel("Cart: 0 items");
            cartLabel.setForeground(Color.WHITE);
            JButton viewCartButton = new JButton("View Cart");
            cartPanel.add(cartLabel);
            cartPanel.add(viewCartButton);
            topPanel.add(cartPanel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            // Results panel
            resultsPanel = new JPanel();
            resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
            JScrollPane scrollPane = new JScrollPane(resultsPanel);
            add(scrollPane, BorderLayout.CENTER);

            // Back button
            JButton backButton = new JButton("⬅ Back to Pharmacy");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("OnlinePharmacyPage"));
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bottomPanel.setOpaque(false);
            bottomPanel.add(backButton);
            add(bottomPanel, BorderLayout.SOUTH);

            // Action Listeners
            loadButton.addActionListener(e -> loadPatientMedicines());
            searchButton.addActionListener(e -> searchMedicines());
            viewCartButton.addActionListener(e -> viewCart());

            // Initially show message to enter patient ID
            showInitialMessage();
        }

        private void showInitialMessage() {
            resultsPanel.removeAll();
            JLabel messageLabel = new JLabel("<html><center>Please enter a Patient ID and click 'Load Patient Medicines'<br>to view prescribed medicines for that patient.</center></html>");
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            resultsPanel.add(messageLabel);
            resultsPanel.revalidate();
            resultsPanel.repaint();
        }

        // Load medicines prescribed to a specific patient
        private void loadPatientMedicines() {
            String patientIdText = patientIdField.getText().trim();
            if (patientIdText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Patient ID.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int patientId;
            try {
                patientId = Integer.parseInt(patientIdText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid Patient ID (numbers only).", "Invalid ID", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if patient exists
            Patient patient = HospitalManagementSystem.getDbManager().getPatientById(patientId);
            if (patient == null) {
                JOptionPane.showMessageDialog(this, "Patient with ID " + patientId + " not found.", "Patient Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get prescribed medicines for this patient
            List<Medicine> patientMedicines = getPatientPrescribedMedicines(patientId);
            displayMedicines(patientMedicines, "Prescribed medicines for Patient: " + patient.getName() + " (ID: " + patientId + ")");
        }

        private void searchMedicines() {
            String patientIdText = patientIdField.getText().trim();
            if (patientIdText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Patient ID first.", "Patient ID Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int patientId;
            try {
                patientId = Integer.parseInt(patientIdText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid Patient ID.", "Invalid ID", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get prescribed medicines for this patient and filter by search term
            List<Medicine> patientMedicines = getPatientPrescribedMedicines(patientId);
            String searchTerm = searchField.getText().toLowerCase();

            List<Medicine> filteredMedicines = new ArrayList<>();
            for (Medicine med : patientMedicines) {
                if (med.getName().toLowerCase().contains(searchTerm)) {
                    filteredMedicines.add(med);
                }
            }

            displayMedicines(filteredMedicines, "Search results for: \"" + searchField.getText() + "\"");
        }

        // Get medicines prescribed to a specific patient
        private List<Medicine> getPatientPrescribedMedicines(int patientId) {
            List<Medicine> patientMedicines = new ArrayList<>();
            Set<String> prescribedMedicineNames = new HashSet<>();

            // Get all consultations for this patient
            List<Consultation> consultations = HospitalManagementSystem.getDbManager().getConsultationsForPatient(patientId);

            // Get all prescribed medicines from all consultations
            for (Consultation consultation : consultations) {
                List<Prescription> prescriptions = HospitalManagementSystem.getDbManager().getPrescriptionsForConsultation(consultation.getId());
                for (Prescription prescription : prescriptions) {
                    // NEW: Capitalize the first letter of medicine name from prescription
                    String capitalizedMedicineName = capitalizeMedicineName(prescription.getMedicineName());
                    prescribedMedicineNames.add(capitalizedMedicineName.toLowerCase()); // Store lowercase for comparison
                }
            }

            // Find these medicines in the medicine inventory
            List<Medicine> allMedicines = HospitalManagementSystem.getDbManager().searchMedicines(""); // Get all medicines
            for (Medicine medicine : allMedicines) {
                if (prescribedMedicineNames.contains(medicine.getName().toLowerCase())) {
                    // Create a new Medicine object with properly capitalized name
                    Medicine capitalizedMedicine = new Medicine(
                            medicine.getId(),
                            capitalizeMedicineName(medicine.getName()),
                            medicine.getPrice(),
                            medicine.getStock()
                    );
                    patientMedicines.add(capitalizedMedicine);
                }
            }

            return patientMedicines;
        }

        // NEW: Helper method to capitalize medicine names properly
        private String capitalizeMedicineName(String medicineName) {
            if (medicineName == null || medicineName.trim().isEmpty()) {
                return medicineName;
            }

            // Split the medicine name by spaces to handle multi-word medicine names
            String[] words = medicineName.trim().split("\\s+");
            StringBuilder capitalizedName = new StringBuilder();

            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (word.length() > 0) {
                    // Capitalize first letter and make rest lowercase
                    String capitalizedWord = word.substring(0, 1).toUpperCase() +
                            word.substring(1).toLowerCase();
                    capitalizedName.append(capitalizedWord);

                    // Add space between words (except for the last word)
                    if (i < words.length - 1) {
                        capitalizedName.append(" ");
                    }
                }
            }

            return capitalizedName.toString();
        }
        private void displayPrescriptions(List<Prescription> prescriptions, int patientId) {
            resultsPanel.removeAll();

            // Get patient info for header
            Patient patient = HospitalManagementSystem.getDbManager().getPatientById(patientId);
            String patientName = patient != null ? patient.getName() : "Unknown";

            // Header
            JLabel header = new JLabel("Prescribed medicines for Patient: " + patientName + " (ID: " + patientId + ")");
            header.setFont(new Font("SansSerif", Font.BOLD, 16));
            header.setBorder(new EmptyBorder(15, 15, 15, 15));
            resultsPanel.add(header);

            // Display each prescription as a medicine item
            for (Prescription p : prescriptions) {
                // Get medicine details from database
                Medicine medicine = HospitalManagementSystem.getDbManager().getMedicineByName(p.getMedicineName());

                if (medicine != null) {
                    JPanel medicinePanel = new JPanel(new BorderLayout(10, 5));
                    medicinePanel.setBackground(Color.WHITE);
                    medicinePanel.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(new Color(200, 200, 200), 1),
                            new EmptyBorder(10, 15, 10, 15)
                    ));
                    medicinePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

                    // Left side - Stock status
                    JLabel stockLabel = new JLabel("✓ In Stock");
                    stockLabel.setForeground(new Color(46, 125, 50));
                    stockLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                    medicinePanel.add(stockLabel, BorderLayout.WEST);

                    // Center - Medicine details
                    JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                    centerPanel.setOpaque(false);

                    JLabel nameLabel = new JLabel(medicine.getName());
                    nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

                    JLabel priceLabel = new JLabel("Price: ₹" + String.format("%.2f", medicine.getPrice()) +
                            " | Stock: " + medicine.getStock());
                    priceLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    priceLabel.setForeground(Color.GRAY);

                    centerPanel.add(nameLabel);
                    centerPanel.add(priceLabel);
                    medicinePanel.add(centerPanel, BorderLayout.CENTER);

                    // Right side - Add to Cart button
                    JButton addToCartBtn = new JButton("Add to Cart");
                    addToCartBtn.setBackground(new Color(33, 150, 243));
                    addToCartBtn.setForeground(Color.WHITE);
                    addToCartBtn.setFocusPainted(false);
                    addToCartBtn.setBorder(new EmptyBorder(5, 15, 5, 15));
                    addToCartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    addToCartBtn.addActionListener(e -> {
                        cartItems.add(medicine);
                        updateCartCount();
                        JOptionPane.showMessageDialog(this,
                                medicine.getName() + " added to cart!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    });

                    medicinePanel.add(addToCartBtn, BorderLayout.EAST);

                    resultsPanel.add(medicinePanel);
                    resultsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }
            resultsPanel.revalidate();
            resultsPanel.repaint();
        }
        private void updateCartCount() {
            if (cartLabel != null) {
                cartLabel.setText("Cart: " + cartItems.size() + " items");
            }
        }
        private void displayMedicines(List<Medicine> medicines, String headerText) {
            resultsPanel.removeAll();

            // Add header
            JLabel headerLabel = new JLabel(headerText);
            headerLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            headerLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            resultsPanel.add(headerLabel);

            if (medicines.isEmpty()) {
                JLabel noMedicinesLabel = new JLabel("No prescribed medicines found for this patient.");
                noMedicinesLabel.setHorizontalAlignment(SwingConstants.CENTER);
                noMedicinesLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
                resultsPanel.add(noMedicinesLabel);
            } else {
                for (Medicine med : medicines) {
                    resultsPanel.add(createMedicinePanel(med));
                }
            }
            resultsPanel.revalidate();
            resultsPanel.repaint();
        }

        // Updated createMedicinePanel method with stock indicator
        private JPanel createMedicinePanel(Medicine med) {
            JPanel panel = new JPanel(new BorderLayout(10, 5));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    new EmptyBorder(10, 10, 10, 10)
            ));
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            // Medicine info - now uses properly capitalized name
            JLabel nameLabel = new JLabel(String.format("<html><b>%s</b><br>Price: ₹%.2f | Stock: %d</html>",
                    med.getName(), med.getPrice(), med.getStock()));
            panel.add(nameLabel, BorderLayout.CENTER);

            // Stock status indicator
            JLabel stockStatus = new JLabel();
            if (med.getStock() > 20) {
                stockStatus.setText("✓ In Stock");
                stockStatus.setForeground(HospitalManagementSystem.COLOR_SUCCESS);
            } else if (med.getStock() > 0) {
                stockStatus.setText("⚠ Low Stock");
                stockStatus.setForeground(Color.ORANGE);
            } else {
                stockStatus.setText("✗ Out of Stock");
                stockStatus.setForeground(HospitalManagementSystem.COLOR_DANGER);
            }
            stockStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
            panel.add(stockStatus, BorderLayout.WEST);

            // Add to cart button
            JButton addToCartButton = new JButton("Add to Cart");
            addToCartButton.addActionListener(e -> addToCart(med));
            if (med.getStock() == 0) {
                addToCartButton.setEnabled(false);
            }
            panel.add(addToCartButton, BorderLayout.EAST);

            return panel;
        }

        private void addToCart(Medicine med) {
            if (med.getStock() > 0) {
                cart.put(med, cart.getOrDefault(med, 0) + 1);
                updateCartLabel();
                JOptionPane.showMessageDialog(this, med.getName() + " added to cart!", "Added to Cart", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Sorry, this item is out of stock.", "Out of Stock", JOptionPane.WARNING_MESSAGE);
            }
        }

        private void updateCartLabel() {
            int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
            cartLabel.setText("Cart: " + totalItems + " items");
        }

        private void viewCart() {
            // Check cartItems (List) instead of cart (Map)
            if (cartItems.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Your cart is empty.",
                        "Empty Cart",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JDialog cartDialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "Shopping Cart", Dialog.ModalityType.APPLICATION_MODAL);
            cartDialog.setSize(400, 300);
            cartDialog.setLayout(new BorderLayout(10, 10));

            JTextArea cartItemsArea = new JTextArea();
            cartItemsArea.setEditable(false);

            double total = 0;
            Map<Medicine, Integer> medicineCount = new HashMap<>();

            // Count quantities of each medicine in cartItems
            for (Medicine med : cartItems) {
                medicineCount.put(med, medicineCount.getOrDefault(med, 0) + 1);
            }

            // Display items with quantities
            for (Map.Entry<Medicine, Integer> entry : medicineCount.entrySet()) {
                Medicine med = entry.getKey();
                int quantity = entry.getValue();
                cartItemsArea.append(String.format("%s x%d - ₹%.2f\n",
                        med.getName(), quantity, med.getPrice() * quantity));
                total += med.getPrice() * quantity;
            }

            cartItemsArea.append("\n" + "=".repeat(30) + "\n");
            cartItemsArea.append(String.format("TOTAL: ₹%.2f", total));

            cartDialog.add(new JScrollPane(cartItemsArea), BorderLayout.CENTER);

            JButton checkoutButton = new JButton("Checkout");
            checkoutButton.addActionListener(e -> {
                cartDialog.dispose();
                // Convert cartItems List to cart Map for checkout
                for (Map.Entry<Medicine, Integer> entry : medicineCount.entrySet()) {
                    cart.put(entry.getKey(), entry.getValue());
                }
                HospitalManagementSystem.showAddressPage(cart);
            });

            cartDialog.add(checkoutButton, BorderLayout.SOUTH);
            cartDialog.setLocationRelativeTo(this);
            cartDialog.setVisible(true);
        }
    }

    public static class PharmacyStockPage extends JPanel {
        private final JTextField searchField;
        private final JPanel resultsPanel;
        private final Map<Medicine, Integer> cart = new HashMap<>();
        private final JLabel cartLabel;
        private List<Medicine> allMedicines;

        public PharmacyStockPage() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            // Top panel for search and cart
            JPanel topPanel = new JPanel(new BorderLayout(10, 10));
            topPanel.setOpaque(false);

            // Search components
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.setOpaque(false);
            searchField = new JTextField(30);
            JButton searchButton = new JButton("Search");
            JButton showAllButton = new JButton("Show All");
            searchPanel.add(new JLabel("Search Medicines:"));
            searchPanel.add(searchField);
            searchPanel.add(searchButton);
            searchPanel.add(showAllButton);

            topPanel.add(searchPanel, BorderLayout.CENTER);

            // Cart components (right section)
            JPanel cartPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            cartPanel.setOpaque(false);
            cartLabel = new JLabel("Cart: 0 items");
            cartLabel.setForeground(Color.WHITE);
            JButton viewCartButton = new JButton("View Cart");
            cartPanel.add(cartLabel);
            cartPanel.add(viewCartButton);
            topPanel.add(cartPanel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            // Results panel
            resultsPanel = new JPanel();
            resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
            JScrollPane scrollPane = new JScrollPane(resultsPanel);
            add(scrollPane, BorderLayout.CENTER);

            // Back button
            JButton backButton = new JButton("⬅ Back to Pharmacy");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("OnlinePharmacyPage"));
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bottomPanel.setOpaque(false);
            bottomPanel.add(backButton);
            add(bottomPanel, BorderLayout.SOUTH);

            // Action Listeners
            searchButton.addActionListener(e -> searchMedicines());
            showAllButton.addActionListener(e -> showAllMedicines());
            viewCartButton.addActionListener(e -> viewCart());

            // Load all medicines initially
            loadAllMedicines();
        }

        private void loadAllMedicines() {
            allMedicines = HospitalManagementSystem.getDbManager().searchMedicines(""); // Get all medicines
            showAllMedicines();
        }

        private void showAllMedicines() {
            displayMedicines(allMedicines, "All Medicines in Pharmacy Stock (" + allMedicines.size() + " items)");
        }

        private void searchMedicines() {
            String searchTerm = searchField.getText().toLowerCase().trim();
            if (searchTerm.isEmpty()) {
                showAllMedicines();
                return;
            }

            List<Medicine> filteredMedicines = new ArrayList<>();
            for (Medicine med : allMedicines) {
                if (med.getName().toLowerCase().contains(searchTerm)) {
                    filteredMedicines.add(med);
                }
            }

            displayMedicines(filteredMedicines, "Search results for: \"" + searchField.getText() + "\" (" + filteredMedicines.size() + " items)");
        }

        private void displayMedicines(List<Medicine> medicines, String headerText) {
            resultsPanel.removeAll();

            // Add header
            JLabel headerLabel = new JLabel(headerText);
            headerLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            headerLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            resultsPanel.add(headerLabel);

            if (medicines.isEmpty()) {
                JLabel noMedicinesLabel = new JLabel("No medicines found matching your search.");
                noMedicinesLabel.setHorizontalAlignment(SwingConstants.CENTER);
                noMedicinesLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
                resultsPanel.add(noMedicinesLabel);
            } else {
                for (Medicine med : medicines) {
                    resultsPanel.add(createMedicinePanel(med));
                }
            }
            resultsPanel.revalidate();
            resultsPanel.repaint();
        }

        private JPanel createMedicinePanel(Medicine med) {
            JPanel panel = new JPanel(new BorderLayout(10, 5));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    new EmptyBorder(10, 10, 10, 10)
            ));
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            // Medicine info
            JLabel nameLabel = new JLabel(String.format("<html><b>%s</b><br>Price: ₹%.2f | Stock: %d</html>",
                    med.getName(), med.getPrice(), med.getStock()));
            panel.add(nameLabel, BorderLayout.CENTER);

            // Stock status indicator
            JLabel stockStatus = new JLabel();
            if (med.getStock() > 20) {
                stockStatus.setText("✓ In Stock");
                stockStatus.setForeground(HospitalManagementSystem.COLOR_SUCCESS);
            } else if (med.getStock() > 0) {
                stockStatus.setText("⚠ Low Stock");
                stockStatus.setForeground(Color.ORANGE);
            } else {
                stockStatus.setText("✗ Out of Stock");
                stockStatus.setForeground(HospitalManagementSystem.COLOR_DANGER);
            }
            stockStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
            panel.add(stockStatus, BorderLayout.WEST);

            // Add to cart button
            JButton addToCartButton = new JButton("Add to Cart");
            addToCartButton.addActionListener(e -> addToCart(med));
            if (med.getStock() == 0) {
                addToCartButton.setEnabled(false);
            }
            panel.add(addToCartButton, BorderLayout.EAST);

            return panel;
        }

        private void addToCart(Medicine med) {
            if (med.getStock() > 0) {
                cart.put(med, cart.getOrDefault(med, 0) + 1);
                updateCartLabel();
                JOptionPane.showMessageDialog(this, med.getName() + " added to cart!", "Added to Cart", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Sorry, this item is out of stock.", "Out of Stock", JOptionPane.WARNING_MESSAGE);
            }
        }

        private void updateCartLabel() {
            int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
            cartLabel.setText("Cart: " + totalItems + " items");
        }

        private void viewCart() {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Your cart is empty.", "Empty Cart", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JDialog cartDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Shopping Cart", Dialog.ModalityType.APPLICATION_MODAL);
            cartDialog.setSize(400, 300);
            cartDialog.setLayout(new BorderLayout(10, 10));

            JTextArea cartItemsArea = new JTextArea();
            cartItemsArea.setEditable(false);
            double total = 0;
            for (Map.Entry<Medicine, Integer> entry : cart.entrySet()) {
                Medicine med = entry.getKey();
                int quantity = entry.getValue();
                cartItemsArea.append(String.format("%s (x%d) - ₹%.2f\n", med.getName(), quantity, med.getPrice() * quantity));
                total += med.getPrice() * quantity;
            }
            cartItemsArea.append("\nTotal: ₹" + String.format("%.2f", total));
            cartDialog.add(new JScrollPane(cartItemsArea), BorderLayout.CENTER);

            JButton checkoutButton = new JButton("Checkout");
            checkoutButton.addActionListener(e -> {
                cartDialog.dispose();
                HospitalManagementSystem.showAddressPage(cart);
            });
            cartDialog.add(checkoutButton, BorderLayout.SOUTH);

            cartDialog.setLocationRelativeTo(this);
            cartDialog.setVisible(true);
        }
    }


    public static class AddressPage extends JPanel {
        private final JTextField nameField, addressField, cityField, pincodeField;

        public AddressPage(Map<Medicine, Integer> cart) {
            setOpaque(false);
            setLayout(new GridBagLayout());
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(new Color(255, 255, 255, 230));
            formPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            // Title
            JLabel titleLabel = new JLabel("Enter Delivery Address");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            titleLabel.setForeground(HospitalManagementSystem.COLOR_FONT_DARK);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            formPanel.add(titleLabel, gbc);

            // Reset constraints for form fields
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;

            // Full Name field
            gbc.gridx = 0;
            gbc.gridy = 1;
            JLabel nameLabel = new JLabel("Full Name:");
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            formPanel.add(nameLabel, gbc);

            nameField = new JTextField(20);
            nameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(nameField, gbc);

            // Address field
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel addressLabel = new JLabel("Street Address:");
            addressLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            formPanel.add(addressLabel, gbc);

            addressField = new JTextField(20);
            addressField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(addressField, gbc);

            // City field
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel cityLabel = new JLabel("City:");
            cityLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            formPanel.add(cityLabel, gbc);

            cityField = new JTextField(20);
            cityField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(cityField, gbc);

            // Pincode field
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel pincodeLabel = new JLabel("Pincode:");
            pincodeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            formPanel.add(pincodeLabel, gbc);

            pincodeField = new JTextField(20);
            pincodeField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(pincodeField, gbc);

            // Buttons panel
            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            buttonsPanel.setOpaque(false);

            // Back button
            JButton backButton = new JButton("← Back to Cart");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.setForeground(Color.BLACK);
            backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            backButton.setBorder(new EmptyBorder(10, 20, 10, 20));
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("OnlinePharmacyPage"));

            // Proceed button with validation
            JButton proceedButton = new JButton("Proceed to Payment");
            proceedButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
            proceedButton.setForeground(Color.WHITE);
            proceedButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            proceedButton.setBorder(new EmptyBorder(10, 20, 10, 20));

            // ADD VALIDATION HERE
            proceedButton.addActionListener(e -> {
                // Validate all fields
                if (!validateFields()) {
                    return; // Don't proceed if validation fails
                }

                // If validation passes, proceed with payment
                String address = String.format("%s, %s, %s - %s",
                        nameField.getText().trim(),
                        addressField.getText().trim(),
                        cityField.getText().trim(),
                        pincodeField.getText().trim());
                HospitalManagementSystem.showPharmacyPaymentPage(cart, address);
            });

            buttonsPanel.add(backButton);
            buttonsPanel.add(proceedButton);

            gbc.gridy = 5;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(20, 10, 10, 10);
            formPanel.add(buttonsPanel, gbc);

            add(formPanel);
        }

        // NEW METHOD: Validate all address fields
        private boolean validateFields() {
            String name = nameField.getText().trim();
            String address = addressField.getText().trim();
            String city = cityField.getText().trim();
            String pincode = pincodeField.getText().trim();

            // Check if any field is empty
            if (name.isEmpty()) {
                showValidationError("Please enter your full name.");
                nameField.requestFocus();
                return false;
            }

            if (address.isEmpty()) {
                showValidationError("Please enter your street address.");
                addressField.requestFocus();
                return false;
            }

            if (city.isEmpty()) {
                showValidationError("Please enter your city.");
                cityField.requestFocus();
                return false;
            }

            if (pincode.isEmpty()) {
                showValidationError("Please enter your pincode.");
                pincodeField.requestFocus();
                return false;
            }

            // Validate pincode format (should be 6 digits)
            if (!pincode.matches("\\d{6}")) {
                showValidationError("Pincode must be exactly 6 digits.");
                pincodeField.requestFocus();
                pincodeField.selectAll();
                return false;
            }

            // Validate name (should contain only letters and spaces)
            if (!name.matches("^[a-zA-Z\\s]+$")) {
                showValidationError("Name should contain only letters and spaces.");
                nameField.requestFocus();
                nameField.selectAll();
                return false;
            }

            // Validate minimum length requirements
            if (name.length() < 2) {
                showValidationError("Name must be at least 2 characters long.");
                nameField.requestFocus();
                nameField.selectAll();
                return false;
            }

            if (address.length() < 5) {
                showValidationError("Address must be at least 5 characters long.");
                addressField.requestFocus();
                addressField.selectAll();
                return false;
            }

            if (city.length() < 2) {
                showValidationError("City name must be at least 2 characters long.");
                cityField.requestFocus();
                cityField.selectAll();
                return false;
            }

            return true; // All validations passed
        }

        // Helper method to show validation error messages
        private void showValidationError(String message) {
            JOptionPane.showMessageDialog(this,
                    message,
                    "Address Validation Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }


    public static class PharmacyPaymentPage extends JPanel {
        private final Map<Medicine, Integer> cart;
        private final String address;

        public PharmacyPaymentPage(Map<Medicine, Integer> cart, String address) {
            this.cart = cart;
            this.address = address;
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            // Main content panel
            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            // Title
            JLabel title = new JLabel("Order Summary", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            contentPanel.add(title, BorderLayout.NORTH);

            // Order details
            JPanel orderPanel = new JPanel(new BorderLayout(10, 10));
            orderPanel.setOpaque(false);

            // Cart items
            JTextArea orderDetails = new JTextArea();
            orderDetails.setEditable(false);
            orderDetails.setFont(new Font("Monospaced", Font.PLAIN, 14));

            StringBuilder orderText = new StringBuilder();
            orderText.append("DELIVERY ADDRESS:\n");
            orderText.append(address).append("\n\n");
            orderText.append("ORDERED MEDICINES:\n");
            orderText.append("-".repeat(50)).append("\n");

            double subtotal = 0;
            for (Map.Entry<Medicine, Integer> entry : cart.entrySet()) {
                Medicine med = entry.getKey();
                int quantity = entry.getValue();
                double itemTotal = med.getPrice() * quantity;
                subtotal += itemTotal;

                orderText.append(String.format("%-20s x%d @ ₹%.2f = ₹%.2f\n",
                        med.getName(), quantity, med.getPrice(), itemTotal));
            }

            orderText.append("-".repeat(50)).append("\n");
            orderText.append(String.format("Subtotal: ₹%.2f\n", subtotal));
            orderText.append(String.format("Delivery Charges: ₹50.00\n"));
            orderText.append(String.format("Tax (5%%): ₹%.2f\n", subtotal * 0.05));
            orderText.append("-".repeat(50)).append("\n");
            orderText.append(String.format("TOTAL: ₹%.2f\n", subtotal + 50 + (subtotal * 0.05)));

            orderDetails.setText(orderText.toString());
            orderPanel.add(new JScrollPane(orderDetails), BorderLayout.CENTER);

            contentPanel.add(orderPanel, BorderLayout.CENTER);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            buttonPanel.setOpaque(false);

            JButton proceedToPaymentButton = new JButton("Proceed to Payment");
            proceedToPaymentButton.setBackground(HospitalManagementSystem.COLOR_SUCCESS);
            proceedToPaymentButton.setForeground(Color.WHITE);
            proceedToPaymentButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            proceedToPaymentButton.setBorder(new EmptyBorder(12, 25, 12, 25));
            proceedToPaymentButton.addActionListener(e -> proceedToPayment());

            JButton backButton = new JButton("⬅ Back to Cart");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            backButton.setBorder(new EmptyBorder(10, 20, 10, 20));
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("OnlinePharmacyPage"));

            buttonPanel.add(backButton);
            buttonPanel.add(proceedToPaymentButton);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(contentPanel, BorderLayout.CENTER);
        }

        private void proceedToPayment() {
            // Calculate total amount including delivery and tax
            double subtotal = 0;
            for (Map.Entry<Medicine, Integer> entry : cart.entrySet()) {
                Medicine med = entry.getKey();
                int quantity = entry.getValue();
                subtotal += med.getPrice() * quantity;
            }

            double deliveryCharges = 50.00;
            double tax = subtotal * 0.05;
            double totalAmount = subtotal + deliveryCharges + tax;

            // Forward to EnhancedOnlinePaymentPage with pharmacy data
            HospitalManagementSystem.showPharmacyEnhancedPaymentPage(cart, address, totalAmount);
        }
    }


    public static class DoctorCalendarPanel extends JPanel {
        private YearMonth currentMonth;

        public DoctorCalendarPanel(Doctor doctor, DateSelectionListener listener) {
            // Make availableDates FINAL so it can be used in ActionListeners
            final List<LocalDate> availableDates = HospitalManagementSystem.getDbManager()
                    .getDoctorAvailableDates(doctor.getId());

            setOpaque(false);
            setLayout(new BorderLayout(10, 10));

            currentMonth = YearMonth.now();

            JPanel navigationPanel = new JPanel(new BorderLayout());
            navigationPanel.setOpaque(false);

            JButton prevButton = new JButton("<");
            JButton nextButton = new JButton(">");
            JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

            navigationPanel.add(prevButton, BorderLayout.WEST);
            navigationPanel.add(monthLabel, BorderLayout.CENTER);
            navigationPanel.add(nextButton, BorderLayout.EAST);

            add(navigationPanel, BorderLayout.NORTH);

            JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 5, 5));
            calendarGrid.setOpaque(false);
            add(calendarGrid, BorderLayout.CENTER);

            // NOW availableDates can be used in these ActionListeners
            prevButton.addActionListener(e -> {
                currentMonth = currentMonth.minusMonths(1);
                updateCalendar(calendarGrid, monthLabel, currentMonth, availableDates, listener);
            });

            nextButton.addActionListener(e -> {
                currentMonth = currentMonth.plusMonths(1);
                updateCalendar(calendarGrid, monthLabel, currentMonth, availableDates, listener);
            });

            updateCalendar(calendarGrid, monthLabel, currentMonth, availableDates, listener);
        }

        private void updateCalendar(JPanel grid, JLabel label, YearMonth month,
                                    List<LocalDate> availableDates, DateSelectionListener listener) {
            grid.removeAll();
            label.setText(month.getMonth().name() + " " + month.getYear());

            String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String day : daysOfWeek) {
                JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
                dayLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
                grid.add(dayLabel);
            }

            LocalDate firstOfMonth = month.atDay(1);
            int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue() % 7;
            for (int i = 0; i < dayOfWeekValue; i++) {
                grid.add(new JLabel(""));
            }

            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                LocalDate date = month.atDay(day);
                JButton dayButton = new JButton(String.valueOf(day));
                dayButton.setFont(new Font("SansSerif", Font.PLAIN, 14));

                // CHANGED: Check if the specific date is in the available dates list
                boolean isAvailable = availableDates.contains(date) &&
                        date.isAfter(LocalDate.now().minusDays(1));

                if (isAvailable) {
                    dayButton.setBackground(new Color(144, 238, 144));
                    dayButton.addActionListener(e -> listener.dateSelected(date));
                } else {
                    dayButton.setBackground(Color.LIGHT_GRAY);
                    dayButton.setEnabled(false);
                }
                grid.add(dayButton);
            }

            grid.revalidate();
            grid.repaint();
        }

        @FunctionalInterface
        interface DateSelectionListener {
            void dateSelected(LocalDate date);
        }
    }

    public static class TimeSlotSelectionPanel extends JPanel {
        public TimeSlotSelectionPanel(Doctor doctor, LocalDate date, TimeSlotSelectionListener listener) {
            setOpaque(false);

            // Use GridLayout with automatic columns to center and wrap properly
            List<LocalTime> timeSlots = HospitalManagementSystem.getDbManager().getAvailableTimeSlotsForDate(doctor.getId(), date);

            if (timeSlots.isEmpty()) {
                setLayout(new FlowLayout(FlowLayout.CENTER));
                add(new JLabel("No available time slots for this day."));
            } else {
                // Calculate optimal columns (8 slots per row works well)
                int slotsPerRow = 8;
                int rows = (int) Math.ceil((double) timeSlots.size() / slotsPerRow);

                setLayout(new GridLayout(rows, slotsPerRow, 10, 10));
                setBorder(new EmptyBorder(20, 50, 20, 50)); // Add padding on sides

                for (LocalTime time : timeSlots) {
                    JButton timeButton = new JButton(time.format(DateTimeFormatter.ofPattern("HH:mm")));
                    timeButton.setBackground(HospitalManagementSystem.COLOR_PRIMARY);
                    timeButton.setForeground(Color.WHITE);
                    timeButton.setFont(new Font("SansSerif", Font.BOLD, 12));
                    timeButton.setBorder(new EmptyBorder(8, 15, 8, 15));
                    timeButton.addActionListener(e -> listener.timeSlotSelected(time));
                    add(timeButton);
                }

                // Fill remaining cells with empty labels to maintain grid
                int totalCells = rows * slotsPerRow;
                int emptySlots = totalCells - timeSlots.size();
                for (int i = 0; i < emptySlots; i++) {
                    add(new JLabel()); // Empty placeholder
                }
            }
        }

        @FunctionalInterface
        interface TimeSlotSelectionListener {
            void timeSlotSelected(LocalTime time);
        }
    }
    public static class DoctorManagementPage extends JPanel {
        private final JPanel doctorsPanel;
        private final List<DoctorTogglePanel> doctorTogglePanels;
        public DoctorManagementPage() {
            this.doctorTogglePanels = new ArrayList<>();
            setOpaque(false);
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));
            // Main content panel
            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBackground(new Color(255, 255, 255, 230));
            contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            // Top panel with title and back button
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
            JLabel title = new JLabel("Doctor Management - Toggle Duty Status", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            topPanel.add(title, BorderLayout.NORTH);
            JButton backButton = new JButton("⬅ Back to Dashboard");
            backButton.setBackground(Color.LIGHT_GRAY);
            backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            backButton.setBorder(new EmptyBorder(8, 15, 8, 15));
            backButton.addActionListener(e -> HospitalManagementSystem.showPage("AdminPage"));
            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            backButtonPanel.setOpaque(false);
            backButtonPanel.add(backButton);
            topPanel.add(backButtonPanel, BorderLayout.EAST);
            contentPanel.add(topPanel, BorderLayout.NORTH);
            // Doctors panel with scroll
            doctorsPanel = new JPanel();
            doctorsPanel.setLayout(new BoxLayout(doctorsPanel, BoxLayout.Y_AXIS));
            doctorsPanel.setOpaque(false);
            JScrollPane scrollPane = new JScrollPane(doctorsPanel);
            scrollPane.setPreferredSize(new Dimension(800, 500));
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            // Refresh button
            JButton refreshButton = new JButton("🔄 Refresh Status");
            refreshButton.setBackground(HospitalManagementSystem.COLOR_SECONDARY);
            refreshButton.setForeground(Color.WHITE);
            refreshButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            refreshButton.setBorder(new EmptyBorder(10, 20, 10, 20));
            refreshButton.addActionListener(e -> refreshDoctorsList());
            JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            refreshPanel.setOpaque(false);
            refreshPanel.add(refreshButton);
            contentPanel.add(refreshPanel, BorderLayout.SOUTH);
            add(contentPanel, BorderLayout.CENTER);
            // Load doctors initially
            loadDoctors();
        }

        private void loadDoctors() {
            doctorsPanel.removeAll();
            doctorTogglePanels.clear();
            List<Doctor> doctors = HospitalManagementSystem.getDbManager().getAllDoctors();

            if (doctors.isEmpty()) {
                JLabel noDocsLabel = new JLabel("No doctors found in the system.", SwingConstants.CENTER);
                noDocsLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
                doctorsPanel.add(noDocsLabel);
            } else {
                for (Doctor doctor : doctors) {
                    DoctorTogglePanel togglePanel = new DoctorTogglePanel(doctor);
                    doctorTogglePanels.add(togglePanel);
                    doctorsPanel.add(togglePanel);
                    doctorsPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing between panels
                }
            }
            doctorsPanel.revalidate();
            doctorsPanel.repaint();
        }

        private void refreshDoctorsList() {
            loadDoctors();
            JOptionPane.showMessageDialog(this, "Doctor status refreshed successfully!", "Refresh Complete", JOptionPane.INFORMATION_MESSAGE);
        }
        // Inner class for individual doctor toggle panels
        // Inner class for individual doctor toggle panels
        private class DoctorTogglePanel extends JPanel {
            private final Doctor doctor;
            private final JToggleButton dutyToggleButton;
            private final JLabel statusLabel;

            public DoctorTogglePanel(Doctor doctor) {
                this.doctor = doctor;
                setLayout(new BorderLayout(10, 10));
                setBackground(new Color(248, 249, 250));
                setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(200, 200, 200), 1),
                        new EmptyBorder(15, 20, 15, 20)
                ));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                // Doctor info panel (left side)
                JPanel doctorInfoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
                doctorInfoPanel.setOpaque(false);
                JLabel nameLabel = new JLabel("Dr. " + doctor.getName());
                nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
                nameLabel.setForeground(HospitalManagementSystem.COLOR_FONT_DARK);
                JLabel specializationLabel = new JLabel("Specialization: " + doctor.getSpecialization());
                specializationLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
                specializationLabel.setForeground(Color.GRAY);
                doctorInfoPanel.add(nameLabel);
                doctorInfoPanel.add(specializationLabel);
                add(doctorInfoPanel, BorderLayout.WEST);
                // Status and toggle panel (right side)
                JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
                controlPanel.setOpaque(false);
                // Status label
                statusLabel = new JLabel();
                statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
                updateStatusLabel();
                // Toggle button
                dutyToggleButton = new JToggleButton();
                dutyToggleButton.setFont(new Font("SansSerif", Font.BOLD, 12));
                dutyToggleButton.setBorder(new EmptyBorder(8, 16, 8, 16));
                dutyToggleButton.setFocusPainted(false);
                dutyToggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                // Set initial state
                boolean isOnDuty = HospitalManagementSystem.getDbManager().isDoctorOnDuty(doctor.getId());
                dutyToggleButton.setSelected(isOnDuty);
                updateToggleButton();
                dutyToggleButton.addActionListener(e -> toggleDutyStatus());
                controlPanel.add(statusLabel);
                controlPanel.add(dutyToggleButton);
                add(controlPanel, BorderLayout.EAST);
            }
            private void toggleDutyStatus() {
                boolean newStatus = dutyToggleButton.isSelected();
                try {
                    // Update database
                    HospitalManagementSystem.getDbManager().updateDoctorDutyStatus(doctor.getId(), newStatus);
                    // Update UI
                    updateToggleButton();
                    updateStatusLabel();
                    // Show confirmation
                    String message = newStatus ?
                            "Dr. " + doctor.getName() + " is now ON DUTY" :
                            "Dr. " + doctor.getName() + " is now OFF DUTY";
                    // Create a small notification instead of a popup
                    showNotification(message);
                } catch (Exception ex) {
                    // Revert toggle if database update failed
                    dutyToggleButton.setSelected(!newStatus);
                    JOptionPane.showMessageDialog(this,
                            "Failed to update duty status for Dr. " + doctor.getName(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            private void updateToggleButton() {
                if (dutyToggleButton.isSelected()) {
                    dutyToggleButton.setText("🟢 ON DUTY");
                    dutyToggleButton.setBackground(HospitalManagementSystem.COLOR_SUCCESS);
                    dutyToggleButton.setForeground(Color.WHITE);
                } else {
                    dutyToggleButton.setText("🔴 OFF DUTY");
                    dutyToggleButton.setBackground(HospitalManagementSystem.COLOR_DANGER);
                    dutyToggleButton.setForeground(Color.WHITE);
                }
            }
            private void updateStatusLabel() {
                boolean isOnDuty = HospitalManagementSystem.getDbManager().isDoctorOnDuty(doctor.getId());
                if (isOnDuty) {
                    statusLabel.setText("Status: Available");
                    statusLabel.setForeground(HospitalManagementSystem.COLOR_SUCCESS);
                } else {
                    statusLabel.setText("Status: Offline");
                    statusLabel.setForeground(HospitalManagementSystem.COLOR_DANGER);
                }
            }
            private void showNotification(String message) {
                // Create a simple notification that disappears after 2 seconds
                JLabel notification = new JLabel(message);
                notification.setOpaque(true);
                notification.setBackground(new Color(76, 175, 80, 200));
                notification.setForeground(Color.WHITE);
                notification.setFont(new Font("SansSerif", Font.BOLD, 12));
                notification.setBorder(new EmptyBorder(5, 10, 5, 10));
                System.out.println("✓ " + message);
            }
        }
    }
}