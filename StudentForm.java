package ui;

import dao.StudentDAO;
import model.Course;
import model.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentForm extends JFrame {

    private JTextField txtPrn, txtName, txtPhone, txtEmail, txtSearch;
    private JComboBox<Course> cmbCourse;
    private JComboBox<Integer> cmbYear;
    private JTable table;
    private DefaultTableModel tableModel;

    private StudentDAO dao = new StudentDAO();
    private int selectedId = -1;   // -1 means no row selected
    private String originalSignature = "";   // form values when a row was selected

    public StudentForm() {
        setTitle("Manage Students");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);  // closes only this window
        setLocationRelativeTo(null);

        // ---------- Form fields (3 rows x 4 columns) ----------
        txtPrn = new JTextField();
        txtName = new JTextField();
        txtPhone = new JTextField();
        txtEmail = new JTextField();
        cmbCourse = new JComboBox<>();
        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});

        JPanel form = new JPanel(new GridLayout(3, 4, 10, 10));
        form.add(new JLabel("PRN:"));    form.add(txtPrn);
        form.add(new JLabel("Name:"));   form.add(txtName);
        form.add(new JLabel("Phone:"));  form.add(txtPhone);
        form.add(new JLabel("Email:"));  form.add(txtEmail);
        form.add(new JLabel("Course:")); form.add(cmbCourse);
        form.add(new JLabel("Year:"));   form.add(cmbYear);

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

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        // ---------- Search bar ----------
        txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Search");
        JButton btnShowAll = new JButton("Show All");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Search (PRN / Name):"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        // ---------- Table ----------
        String[] columns = {"ID", "PRN", "Name", "Phone", "Email", "Course", "Year"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;   // user cannot type inside the table
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        center.add(searchPanel, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // ---------- Actions ----------
        btnAdd.addActionListener(e -> addStudent());
        btnUpdate.addActionListener(e -> updateStudent());
        btnDelete.addActionListener(e -> deleteStudent());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            loadTable("");
        });

        // Clicking a table row fills the form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                fillFormFromRow(table.getSelectedRow());
            }
        });

        // ---------- Initial data ----------
        loadCourses();
        loadTable("");
    }

    // ================= Helper methods =================

    private void loadCourses() {
        try {
            for (Course c : dao.getAllCourses()) {
                cmbCourse.addItem(c);
            }
            if (cmbCourse.getItemCount() == 0) {
                showError("No courses found. Insert courses into the 'course' table first.");
            }
        } catch (Exception ex) {
            showError("Could not load courses: " + ex.getMessage());
        }
    }

    private void loadTable(String keyword) {
        tableModel.setRowCount(0);   // clear old rows
        try {
            List<Student> list = dao.search(keyword);
            for (Student s : list) {
                tableModel.addRow(new Object[]{
                        s.getStudentId(), s.getPrn(), s.getName(), s.getPhone(),
                        s.getEmail(), s.getCourseName(), s.getYear()});
            }
        } catch (Exception ex) {
            showError("Could not load students: " + ex.getMessage());
        }
    }

    private void fillFormFromRow(int row) {
        selectedId = (int) tableModel.getValueAt(row, 0);
        txtPrn.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        txtName.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        txtPhone.setText(valueOrEmpty(tableModel.getValueAt(row, 3)));
        txtEmail.setText(valueOrEmpty(tableModel.getValueAt(row, 4)));

        String courseName = String.valueOf(tableModel.getValueAt(row, 5));
        for (int i = 0; i < cmbCourse.getItemCount(); i++) {
            if (cmbCourse.getItemAt(i).getCourseName().equals(courseName)) {
                cmbCourse.setSelectedIndex(i);
                break;
            }
        }
        cmbYear.setSelectedItem(tableModel.getValueAt(row, 6));
        originalSignature = formSignature();
    }

    private String valueOrEmpty(Object o) {
        return o == null ? "" : o.toString();
    }

    // All form values joined into one string, used to detect "no changes"
    private String formSignature() {
        return txtPrn.getText().trim() + "|" + txtName.getText().trim() + "|"
                + txtPhone.getText().trim() + "|" + txtEmail.getText().trim() + "|"
                + cmbCourse.getSelectedItem() + "|" + cmbYear.getSelectedItem();
    }

    private void clearForm() {
        selectedId = -1;
        txtPrn.setText("");
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        if (cmbCourse.getItemCount() > 0) cmbCourse.setSelectedIndex(0);
        cmbYear.setSelectedIndex(0);
        table.clearSelection();
    }

    // Reads the form and builds a Student object; returns null if invalid
    private Student readForm(int id) {
        String prn = txtPrn.getText().trim();
        String name = txtName.getText().trim();
        Course course = (Course) cmbCourse.getSelectedItem();

        if (prn.isEmpty() || name.isEmpty()) {
            showError("PRN and Name are required.");
            return null;
        }
        if (course == null) {
            showError("Please select a course.");
            return null;
        }
        return new Student(id, prn, name, txtPhone.getText().trim(), txtEmail.getText().trim(),
                course.getCourseId(), course.getCourseName(), (int) cmbYear.getSelectedItem());
    }

    // ================= Button actions =================

    private void addStudent() {
        Student s = readForm(0);
        if (s == null) return;
        try {
            dao.addStudent(s);
            JOptionPane.showMessageDialog(this, "Student added successfully.");
            clearForm();
            loadTable("");
        } catch (Exception ex) {
            showError("Could not add student (is the PRN already used?): " + ex.getMessage());
        }
    }

    private void updateStudent() {
        if (selectedId == -1) {
            showError("Select a student from the table first.");
            return;
        }
        if (formSignature().equals(originalSignature)) {
            JOptionPane.showMessageDialog(this, "No changes were made.");
            return;
        }
        Student s = readForm(selectedId);
        if (s == null) return;
        try {
            dao.updateStudent(s);
            JOptionPane.showMessageDialog(this, "Student updated successfully.");
            clearForm();
            loadTable("");
        } catch (Exception ex) {
            showError("Could not update student: " + ex.getMessage());
        }
    }

    private void deleteStudent() {
        if (selectedId == -1) {
            showError("Select a student from the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this student?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            dao.deleteStudent(selectedId);
            clearForm();
            loadTable("");
        } catch (Exception ex) {
            showError("Could not delete (student may have payments): " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}