package ui;

import dao.DuesDAO;
import dao.StudentDAO;
import model.Course;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.List;

public class DuesReportFrame extends JFrame {

    private JComboBox<Course> cmbCourse;
    private JComboBox<String> cmbYear;
    private JCheckBox chkPending;
    private JTextField txtSearch;
    private JLabel lblSummary;
    private JTable table;
    private DefaultTableModel tableModel;

    private DuesDAO duesDao = new DuesDAO();
    private StudentDAO studentDao = new StudentDAO();   // reused for the course list

    public DuesReportFrame() {
        setTitle("Dues Report");
        setSize(950, 620);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // ---------- Filters ----------
        cmbCourse = new JComboBox<>();
        cmbCourse.addItem(new Course(0, "All Courses"));   // id 0 = no course filter
        cmbYear = new JComboBox<>(new String[]{"All Years", "1", "2", "3", "4"});
        chkPending = new JCheckBox("Pending dues only");
        txtSearch = new JTextField(14);
        JButton btnShow = new JButton("Show");
        JButton btnPrint = new JButton("Print Report");

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filters.setBorder(BorderFactory.createTitledBorder("Filters"));
        filters.add(new JLabel("Course:"));
        filters.add(cmbCourse);
        filters.add(new JLabel("Year:"));
        filters.add(cmbYear);
        filters.add(chkPending);
        filters.add(new JLabel("PRN / Name:"));
        filters.add(txtSearch);
        filters.add(btnShow);
        filters.add(btnPrint);

        // ---------- Table ----------
        String[] columns = {"PRN", "Name", "Course", "Year",
                            "Total Fee", "Paid", "Dues", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);

        // Colour the Status column: red = Pending, green = Paid
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                String s = String.valueOf(value);
                if (!selected) {
                    if (s.equals("Pending")) c.setForeground(Color.RED);
                    else if (s.equals("Paid")) c.setForeground(new Color(0, 128, 0));
                    else c.setForeground(Color.DARK_GRAY);
                }
                return c;
            }
        });

        // ---------- Summary ----------
        lblSummary = new JLabel(" ", SwingConstants.CENTER);
        lblSummary.setFont(new Font("Arial", Font.BOLD, 14));
        lblSummary.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
        north.add(filters, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(lblSummary, BorderLayout.SOUTH);

        // ---------- Load data first, then attach listeners ----------
        loadCourses();
        loadReport();

        btnShow.addActionListener(e -> loadReport());
        txtSearch.addActionListener(e -> loadReport());
        cmbCourse.addActionListener(e -> loadReport());
        cmbYear.addActionListener(e -> loadReport());
        chkPending.addActionListener(e -> loadReport());
        btnPrint.addActionListener(e -> printReport());
    }

    private void loadCourses() {
        try {
            for (Course c : studentDao.getAllCourses()) {
                cmbCourse.addItem(c);
            }
        } catch (Exception ex) {
            showError("Could not load courses: " + ex.getMessage());
        }
    }

    private void loadReport() {
        Course course = (Course) cmbCourse.getSelectedItem();
        int courseId = (course == null) ? 0 : course.getCourseId();
        int year = cmbYear.getSelectedIndex();   // 0 = all years, 1 to 4 = that year

        tableModel.setRowCount(0);
        try {
            List<Object[]> rows = duesDao.getDues(courseId, year,
                    chkPending.isSelected(), txtSearch.getText().trim());

            double sumFee = 0, sumPaid = 0, sumDues = 0;

            for (Object[] r : rows) {
                double total = (double) r[4];
                double paid = (double) r[5];
                double dues = Math.max((double) r[6], 0);

                String status;
                if (total <= 0) status = "No fee set";
                else if (dues > 0.001) status = "Pending";
                else status = "Paid";

                sumFee += total;
                sumPaid += paid;
                sumDues += dues;

                tableModel.addRow(new Object[]{
                        r[0], r[1], r[2], r[3],
                        String.format("%.2f", total),
                        String.format("%.2f", paid),
                        String.format("%.2f", dues),
                        status});
            }

            lblSummary.setText(String.format(
                    "Students: %d   |   Total Fee: Rs. %.2f   |   Collected: Rs. %.2f   |   Pending Dues: Rs. %.2f",
                    rows.size(), sumFee, sumPaid, sumDues));
        } catch (Exception ex) {
            showError("Could not load report: " + ex.getMessage());
        }
    }

    private void printReport() {
        try {
            table.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat("College Fee Dues Report"),
                    new MessageFormat("Page {0}"));
        } catch (PrinterException ex) {
            showError("Could not print: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}