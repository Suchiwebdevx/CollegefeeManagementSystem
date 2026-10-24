package dao;

import db.DBConnection;
import model.Course;
import model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    // For the Course dropdown
    public List<Course> getAllCourses() throws SQLException {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT course_id, course_name FROM course ORDER BY course_name";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Course(rs.getInt("course_id"), rs.getString("course_name")));
            }
        }
        return list;
    }

    public List<Student> getAllStudents() throws SQLException {
        return search("");   // empty keyword matches everyone
    }

    // Search by PRN or name
    public List<Student> search(String keyword) throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.student_id, s.prn, s.name, s.phone, s.email, "
                   + "s.course_id, c.course_name, s.year "
                   + "FROM student s JOIN course c ON s.course_id = c.course_id "
                   + "WHERE s.prn LIKE ? OR s.name LIKE ? ORDER BY s.student_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("prn"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getInt("course_id"),
                        rs.getString("course_name"),
                        rs.getInt("year")));
            }
        }
        return list;
    }

    public void addStudent(Student s) throws SQLException {
        String sql = "INSERT INTO student (prn, name, phone, email, course_id, year) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getPrn());
            ps.setString(2, s.getName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setInt(5, s.getCourseId());
            ps.setInt(6, s.getYear());
            ps.executeUpdate();
        }
    }

    public void updateStudent(Student s) throws SQLException {
        String sql = "UPDATE student SET prn=?, name=?, phone=?, email=?, course_id=?, year=? "
                   + "WHERE student_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getPrn());
            ps.setString(2, s.getName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setInt(5, s.getCourseId());
            ps.setInt(6, s.getYear());
            ps.setInt(7, s.getStudentId());
            ps.executeUpdate();
        }
    }

    public void deleteStudent(int studentId) throws SQLException {
        String sql = "DELETE FROM student WHERE student_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        }
    }


}
 
