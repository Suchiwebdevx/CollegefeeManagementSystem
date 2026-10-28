package ui;

import dao.FeeDAO;
import dao.StudentDAO;
import model.Course;
import model.FeeStructure;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class FeeStructureForm extends JFrame {

    private JComboBox<Course> cmbCourse;
    private JComboBox<Integer> cmbYear;
    private JComboBox<String> cmbFeeHead;
    private JTextField txtAmount;
    private JCheckBox chkAll;
    private JLabel lblTotal;
    private JTable table;
    private DefaultTableModel tableModel;

    private FeeDAO feeDao = new FeeDAO();
    private StudentDAO studentDao = new StudentDAO();   // reused for the course list
    private int selectedId = -1;                        // -1 means no row selected
    private String originalSignature = "";

    public FeeStructureForm() {
        setTitle("Fee Structure");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // ---------- Form fields ----------
        cmbCourse = new JComboBox<>();
        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        cmbFeeHead = new JComboBox<>(new String[]{
                "Tuition", "Exam", "Library", "Laboratory", "Development", "Sports",
                "Additional Fee", "Other"});
        cmbFeeHead.setEditable(true);   // you can also type a new fee head
        txtAmount = new JTextField();
        chkAll = new JCheckBox("Apply to all courses & years");

        JPanel form = new JPanel(new GridLayout(3, 4, 10, 10));
        form.add(new JLabel("Course:"));    form.add(cmbCourse);
        form.add(new JLabel("Year:"));      form.add(cmbYear);
        form.add(new JLabel("Fee Head:"));  form.add(cmbFeeHead);
        form.add(new JLabel("Amount (Rs.):")); form.add(txtAmount);
        form.add(new JLabel(""));  form.add(chkAll);
        form.add(new JLabel(""));  form.add(new JLabel(""));

        // ---------- Buttons ----------
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        // ---------- Total label ----------
        lblTotal = new JLabel("Total fee for this course & year: Rs. 0.00", SwingConstants.CENTER);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel top = new JPanel(new BorderLayout(5, 10));
        top.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        top.add(form, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.CENTER);
        top.add(lblTotal, BorderLayout.SOUTH);

        // ---------- Table ----------
        String[] columns = {"ID", "Course", "Year", "Fee Head", "Amount (Rs.)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // ---------- Load data first, then attach listeners ----------
        loadCourses();
        loadTable();
        updateTotal();

        cmbCourse.addActionListener(e -> updateTotal());
        cmbYear.addActionListener(e -> updateTotal());

        btnAdd.addActionListener(e -> addFee());
        btnUpdate.addActionListener(e -> updateFee());
        btnDelete.addActionListener(e -> deleteFee());
        btnClear.addActionListener(e -> clearForm());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                fillFormFromRow(table.getSelectedRow());
            }
        });
    }

    // ================= Helper methods =================

    private void loadCourses() {
        try {
            for (Course c : studentDao.getAllCourses()) {
                cmbCourse.addItem(c);
            }
        } catch (Exception ex) {
            showError("Could not load courses: " + ex.getMessage());
        }
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            for (FeeStructure f : feeDao.getAll()) {
                tableModel.addRow(new Object[]{
                        f.getFeeId(), f.getCourseName(), f.getYear(),
                        f.getFeeHead(), String.format("%.2f", f.getAmount())});
            }
        } catch (Exception ex) {
            showError("Could not load fee structure: " + ex.getMessage());
        }
    }

    // Shows the total of all fee heads for the selected course and year
    private void updateTotal() {
        Course c = (Course) cmbCourse.getSelectedItem();
        if (c == null) return;
        try {
            double total = feeDao.getTotal(c.getCourseId(), (int) cmbYear.getSelectedItem());
            lblTotal.setText(String.format("Total fee for this course & year: Rs. %.2f", total));
        } catch (Exception ex) {
            showError("Could not calculate total: " + ex.getMessage());
        }
    }

    private String feeHeadText() {
        return cmbFeeHead.getEditor().getItem().toString().trim();
    }

    private String formSignature() {
        return cmbCourse.getSelectedItem() + "|" + cmbYear.getSelectedItem() + "|"
                + feeHeadText() + "|" + txtAmount.getText().trim();
    }

    private void fillFormFromRow(int row) {
        selectedId = (int) tableModel.getValueAt(row, 0);

        String courseName = String.valueOf(tableModel.getValueAt(row, 1));
        for (int i = 0; i < cmbCourse.getItemCount(); i++) {
            if (cmbCourse.getItemAt(i).getCourseName().equals(courseName)) {
                cmbCourse.setSelectedIndex(i);
                break;
            }
        }
        cmbYear.setSelectedItem(tableModel.getValueAt(row, 2));
        cmbFeeHead.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 3)));
        txtAmount.setText(String.valueOf(tableModel.getValueAt(row, 4)));
        originalSignature = formSignature();
    }

    private void clearForm() {
        selectedId = -1;
        originalSignature = "";
        txtAmount.setText("");
        chkAll.setSelected(false);
        cmbFeeHead.setSelectedIndex(0);
        table.clearSelection();
    }

    // Reads and validates the form; returns null if something is wrong
    private FeeStructure readForm(int id) {
        Course course = (Course) cmbCourse.getSelectedItem();
        String head = feeHeadText();

        if (course == null) {
            showError("Please select a course.");
            return null;
        }
        if (head.isEmpty()) {
            showError("Please enter a fee head.");
            return null;
        }
        double amount;
        try {
            // accept inputs like 65,600 by removing commas and spaces first
            amount = Double.parseDouble(txtAmount.getText().replace(",", "").replace(" ", ""));
        } catch (NumberFormatException ex) {
            showError("Please enter a valid amount (numbers only).");
            return null;
        }
        if (amount <= 0) {
            showError("Amount must be greater than zero.");
            return null;
        }
        return new FeeStructure(id, course.getCourseId(), course.getCourseName(),
                (int) cmbYear.getSelectedItem(), head, amount);
    }

    // ================= Button actions =================

    private void addFee() {
        FeeStructure f = readForm(0);
        if (f == null) return;
        if (chkAll.isSelected()) {
            addToAll(f);
            return;
        }
        try {
            if (feeDao.exists(f.getCourseId(), f.getYear(), f.getFeeHead(), 0)) {
                showError("This fee head already exists for this course and year.\n"
                        + "Select its row in the table and use Update instead.");
                return;
            }
            feeDao.add(f);
            JOptionPane.showMessageDialog(this, "Fee added successfully.");
            clearForm();
            loadTable();
            updateTotal();
        } catch (Exception ex) {
            showError("Could not add fee: " + ex.getMessage());
        }
    }

    // Adds the fee head to every course and every year after confirmation
    private void addToAll(FeeStructure f) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Add \"" + f.getFeeHead() + "\" of Rs. " + String.format("%.2f", f.getAmount())
                + " to ALL courses and all their years?\n"
                + "(Rows that already have this fee head are skipped.)",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            int added = feeDao.addForAll(f.getFeeHead(), f.getAmount());
            JOptionPane.showMessageDialog(this,
                    "Fee added for " + added + " course-year combinations.");
            clearForm();
            loadTable();
            updateTotal();
        } catch (Exception ex) {
            showError("Could not add fee to all: " + ex.getMessage());
        }
    }

    private void updateFee() {
        if (selectedId == -1) {
            showError("Select a fee row from the table first.");
            return;
        }
        if (formSignature().equals(originalSignature)) {
            JOptionPane.showMessageDialog(this, "No changes were made.");
            return;
        }
        FeeStructure f = readForm(selectedId);
        if (f == null) return;
        try {
            if (feeDao.exists(f.getCourseId(), f.getYear(), f.getFeeHead(), selectedId)) {
                showError("Another row with this fee head already exists for this course and year.");
                return;
            }
            feeDao.update(f);
            JOptionPane.showMessageDialog(this, "Fee updated successfully.");
            clearForm();
            loadTable();
            updateTotal();
        } catch (Exception ex) {
            showError("Could not update fee: " + ex.getMessage());
        }
    }

    private void deleteFee() {
        if (selectedId == -1) {
            showError("Select a fee row from the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this fee head?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            feeDao.delete(selectedId);
            clearForm();
            loadTable();
            updateTotal();
        } catch (Exception ex) {
            showError("Could not delete fee: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}