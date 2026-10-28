package ui;

import dao.FeeDAO;
import dao.PaymentDAO;
import model.Payment;
import model.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentForm extends JFrame {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private JTextField txtPrn, txtAmount, txtRemarks;
    private JComboBox<String> cmbMode;
    private JLabel lblName, lblCourse, lblYear, lblTotal, lblPaid, lblDues;
    private JTable table;
    private DefaultTableModel tableModel;

    private PaymentDAO paymentDao = new PaymentDAO();
    private FeeDAO feeDao = new FeeDAO();

    private Student currentStudent = null;
    private double totalFee = 0;
    private double dues = 0;
    private List<Payment> history = new ArrayList<>();   // same order as the table rows

    public PaymentForm() {
        setTitle("Collect Fee / Receipt");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // ---------- 1. Find student ----------
        txtPrn = new JTextField(18);
        JButton btnFind = new JButton("Find");
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Find Student"));
        searchPanel.add(new JLabel("Student PRN:"));
        searchPanel.add(txtPrn);
        searchPanel.add(btnFind);

        // ---------- 2. Student and fee summary ----------
        lblName = new JLabel("-");
        lblCourse = new JLabel("-");
        lblYear = new JLabel("-");
        lblTotal = new JLabel("-");
        lblPaid = new JLabel("-");
        lblDues = new JLabel("-");
        lblDues.setFont(lblDues.getFont().deriveFont(Font.BOLD));

        JPanel infoPanel = new JPanel(new GridLayout(3, 4, 10, 8));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Student & Fee Summary"));
        infoPanel.add(new JLabel("Name:"));       infoPanel.add(lblName);
        infoPanel.add(new JLabel("Course:"));     infoPanel.add(lblCourse);
        infoPanel.add(new JLabel("Year:"));       infoPanel.add(lblYear);
        infoPanel.add(new JLabel("Total Fee:"));  infoPanel.add(lblTotal);
        infoPanel.add(new JLabel("Paid So Far:")); infoPanel.add(lblPaid);
        infoPanel.add(new JLabel("Pending Dues:")); infoPanel.add(lblDues);

        // ---------- 3. Collect payment ----------
        txtAmount = new JTextField(10);
        cmbMode = new JComboBox<>(new String[]{"Cash", "UPI", "Card", "Cheque", "Net Banking"});
        txtRemarks = new JTextField(15);
        JButton btnCollect = new JButton("Collect Fee");

        JPanel payPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        payPanel.setBorder(BorderFactory.createTitledBorder("Collect Payment"));
        payPanel.add(new JLabel("Amount (Rs.):"));
        payPanel.add(txtAmount);
        payPanel.add(new JLabel("Mode:"));
        payPanel.add(cmbMode);
        payPanel.add(new JLabel("Remarks:"));
        payPanel.add(txtRemarks);
        payPanel.add(btnCollect);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
        top.add(searchPanel);
        top.add(infoPanel);
        top.add(payPanel);

        // ---------- 4. Payment history ----------
        String[] columns = {"Receipt No", "Date", "Amount (Rs.)", "Mode", "Remarks"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);

        JButton btnView = new JButton("View / Print Receipt");
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomButtons.add(btnView);

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(BorderFactory.createTitledBorder("Payment History"));
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(bottomButtons, BorderLayout.SOUTH);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        centerWrap.add(center, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(centerWrap, BorderLayout.CENTER);

        // ---------- Actions ----------
        btnFind.addActionListener(e -> findStudent());
        txtPrn.addActionListener(e -> findStudent());   // Enter key also searches
        btnCollect.addActionListener(e -> collectFee());
        btnView.addActionListener(e -> viewSelectedReceipt());
    }

    // ================= Student lookup =================

    private void findStudent() {
        String prn = txtPrn.getText().trim();
        if (prn.isEmpty()) {
            showError("Please enter a PRN.");
            return;
        }
        try {
            Student s = paymentDao.findByPrn(prn);
            if (s == null) {
                clearStudent();
                showError("No student found with PRN " + prn + ".");
                return;
            }
            currentStudent = s;
            refreshStudentData();
        } catch (Exception ex) {
            showError("Could not find student: " + ex.getMessage());
        }
    }

    private void clearStudent() {
        currentStudent = null;
        totalFee = 0;
        dues = 0;
        history = new ArrayList<>();
        lblName.setText("-");
        lblCourse.setText("-");
        lblYear.setText("-");
        lblTotal.setText("-");
        lblPaid.setText("-");
        lblDues.setText("-");
        tableModel.setRowCount(0);
    }

    // Reloads fee, payments, labels and the history table for the current student
    private void refreshStudentData() throws Exception {
        lblName.setText(currentStudent.getName());
        lblCourse.setText(currentStudent.getCourseName());
        lblYear.setText(String.valueOf(currentStudent.getYear()));

        totalFee = feeDao.getTotal(currentStudent.getCourseId(), currentStudent.getYear());
        history = paymentDao.getPayments(currentStudent.getStudentId());

        double paid = 0;
        for (Payment p : history) {
            paid += p.getAmountPaid();
        }
        dues = totalFee - paid;

        lblTotal.setText(totalFee > 0
                ? String.format("Rs. %.2f", totalFee)
                : "Rs. 0.00 (no fee set)");
        lblPaid.setText(String.format("Rs. %.2f", paid));
        lblDues.setText(String.format("Rs. %.2f", Math.max(dues, 0)));
        lblDues.setForeground(dues > 0.001 ? Color.RED : new Color(0, 128, 0));

        tableModel.setRowCount(0);
        for (Payment p : history) {
            tableModel.addRow(new Object[]{
                    p.getReceiptNo(), p.getPaymentDate().format(DATE_FMT),
                    String.format("%.2f", p.getAmountPaid()), p.getMode(), p.getRemarks()});
        }
    }

    // ================= Collect fee =================

    private void collectFee() {
        if (currentStudent == null) {
            showError("Find a student by PRN first.");
            return;
        }
        if (totalFee <= 0) {
            showError("No fee structure is set for this course and year.\n"
                    + "Add it in the Fee Structure screen first.");
            return;
        }
        if (dues <= 0.001) {
            showError("All fees are already paid for this student.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(txtAmount.getText().replace(",", "").replace(" ", ""));
        } catch (NumberFormatException ex) {
            showError("Please enter a valid amount (numbers only).");
            return;
        }
        if (amount <= 0) {
            showError("Amount must be greater than zero.");
            return;
        }
        if (amount > dues + 0.001) {
            showError(String.format("Amount is more than the pending dues (Rs. %.2f).", dues));
            return;
        }

        Payment p = new Payment(0, currentStudent.getStudentId(), LocalDate.now(), amount,
                (String) cmbMode.getSelectedItem(), txtRemarks.getText().trim());
        try {
            int receiptNo = paymentDao.addPayment(p);
            txtAmount.setText("");
            txtRemarks.setText("");
            refreshStudentData();
            for (Payment h : history) {
                if (h.getReceiptNo() == receiptNo) {
                    showReceipt(h);
                    break;
                }
            }
        } catch (Exception ex) {
            showError("Could not save payment: " + ex.getMessage());
        }
    }

    // ================= Receipt =================

    private void viewSelectedReceipt() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Select a payment from the history table first.");
            return;
        }
        showReceipt(history.get(row));
    }

    private void showReceipt(Payment p) {
        // Total paid up to and including this receipt
        double paidTillNow = 0;
        for (Payment h : history) {
            if (h.getReceiptNo() <= p.getReceiptNo()) {
                paidTillNow += h.getAmountPaid();
            }
        }
        double balance = totalFee - paidTillNow;

        String line = "=".repeat(46);
        String dash = "-".repeat(46);
        String remarks = (p.getRemarks() == null || p.getRemarks().isEmpty()) ? "-" : p.getRemarks();

        String text = String.join("\n",
                line,
                "            COLLEGE FEE RECEIPT",
                line,
                String.format("%-16s: %d", "Receipt No", p.getReceiptNo()),
                String.format("%-16s: %s", "Date", p.getPaymentDate().format(DATE_FMT)),
                dash,
                String.format("%-16s: %s", "Student", currentStudent.getName()),
                String.format("%-16s: %s", "PRN", currentStudent.getPrn()),
                String.format("%-16s: %s", "Course", currentStudent.getCourseName()),
                String.format("%-16s: %d", "Year", currentStudent.getYear()),
                dash,
                String.format("%-16s: Rs. %.2f", "Total Fee", totalFee),
                String.format("%-16s: Rs. %.2f", "Amount Paid Now", p.getAmountPaid()),
                String.format("%-16s: Rs. %.2f", "Paid Till Date", paidTillNow),
                String.format("%-16s: Rs. %.2f", "Balance Due", Math.max(balance, 0)),
                String.format("%-16s: %s", "Payment Mode", p.getMode()),
                String.format("%-16s: %s", "Remarks", remarks),
                line,
                "  Thank you. This is a computer-generated",
                "  receipt.",
                line);

        JTextArea area = new JTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setEditable(false);
        area.setMargin(new Insets(10, 15, 10, 15));

        JButton btnPrint = new JButton("Print");
        JButton btnClose = new JButton("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        buttons.add(btnPrint);
        buttons.add(btnClose);

        JDialog dialog = new JDialog(this, "Receipt No. " + p.getReceiptNo(), true);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);

        btnPrint.addActionListener(e -> {
            try {
                area.print();
            } catch (PrinterException ex) {
                showError("Could not print: " + ex.getMessage());
            }
        });
        btnClose.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}