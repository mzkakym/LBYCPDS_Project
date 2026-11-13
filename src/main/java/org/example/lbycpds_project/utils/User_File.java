package org.example.lbycpds_project.utils;

import org.example.lbycpds_project.models.User;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class User_File {
    private static final String FILE_PATH = "src/main/resources/records/users.txt";

    // SEARCH FOR USER BY USER ID
    public static User findUser(String userID) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] data = line.split("\\|", -1);
                if (data.length != 7) {
                    continue;
                }
                if (data[0].trim().equalsIgnoreCase(userID)) {
                    return new User(
                            data[0].trim(), // user ID
                            data[1].trim(), // password
                            data[2].trim(), // role
                            data[3].trim(), // name
                            data[4].trim(), // college department
                            data[5].trim(), // email
                            data[6].trim() // verification
                    );
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading users.txt: " + e.getMessage());
        }
        return null;
    }

    // UPDATE USER PASSWORD
    public static boolean updatePassword(String userID, String newPassword) {
        List<String> records = new ArrayList<>();
        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split("\\|", -1);
                if (data.length == 7 && data[0].trim().equalsIgnoreCase(userID)) {
                    data[1] = newPassword;
                    found = true;
                    line = String.join("|", data);
                }
                records.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading users.txt: " + e.getMessage());
            return false;
        }
        if (!found) {
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String record : records) {
                writer.write(record);
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error updating users.txt: " + e.getMessage());
            return false;
        }
    }


    // ADD NEW USER
    public static boolean addUser(String userID, String password, String role, String fullName, String department, String contact, String status) {
        String record =  userID + "|" + password + "|" + role + "|" + fullName + "|" + department + "|" + contact + "|" + status;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.newLine();
            writer.write(record);
            return true;
        } catch (IOException e) {
            System.out.println("Error saving new user: " + e.getMessage());
            return false;
        }
    }

}