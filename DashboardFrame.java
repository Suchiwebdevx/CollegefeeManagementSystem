package ui;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    public DashboardFrame() {
        setTitle("College Fee Management - Dashboard");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // BorderLayout: title on top (NORTH), buttons in the middle (CENTER)
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("College Fee Management System", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        root.add(title, BorderLayout.NORTH);

        // 5 rows x 1 column of buttons
        JPanel menu = new JPanel(new GridLayout(5, 1, 10, 10));
        JButton btnStudents = new JButton("Manage Students");
        JButton btnFees = new JButton("Fee Structure");
        JButton btnPayment = new JButton("Collect Fee / Receipt");
        JButton btnDues = new JButton("Dues Report");
        JButton btnLogout = new JButton("Logout");

        menu.add(btnStudents);
        menu.add(btnFees);
        menu.add(btnPayment);
        menu.add(btnDues);
        menu.add(btnLogout);
        root.add(menu, BorderLayout.CENTER);

        add(root);

        btnStudents.addActionListener(e -> new StudentForm().setVisible(true));
        btnFees.addActionListener(e -> new FeeStructureForm().setVisible(true));
        btnPayment.addActionListener(e -> new PaymentForm().setVisible(true));
        btnDues.addActionListener(e -> new DuesReportFrame().setVisible(true));

        btnLogout.addActionListener(e -> {
            dispose();                          // close dashboard
            new LoginFrame().setVisible(true);  // back to login
        });
    }
}