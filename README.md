# Electronics Inventory System (EIS)

The **Electronics Inventory System (EIS)** is a Java-based desktop application designed to help manage electronic equipment and inventory within a laboratory. It provides a centralized system for managing equipment records, user accounts, and borrowing requests while supporting different levels of access depending on the user's role.

The project is developed as part of the **LBYCPDS** course.

## Features

- **User Authentication** – Allows registered users to log in using their account credentials.
- **Account Activation** – Provides an account activation process for users.
- **Role-Based Access** – Provides different interfaces and functionalities for Borrowers, Laboratory Staff, and Administrators.
- **Equipment Management** – Organizes and maintains electronic equipment records and available quantities.
- **Borrowing Requests** – Allows borrowers to submit equipment requests and track their request status.
- **Inventory Monitoring** – Keeps track of equipment quantities and availability.
- **Data Management** – Uses a centralized local file-based database to store and manage application records.

## User Roles

| Role | Responsibilities |
|---|---|
| **Borrower** | Accesses the system, views available equipment, and submits borrowing requests. |
| **Laboratory Staff** | Manages equipment records, monitors inventory, and handles borrowing and return requests. |
| **Administrator** | Oversees user accounts, system access, and administrative operations. |

## Technologies Used

- **Java** – Main programming language.
- **JavaFX** – Desktop graphical user interface.
- **FXML** – Defines the layouts and structure of application screens.
- **Maven** – Project build and dependency management.
- **CSV / Local Files** – Stores application data, including user accounts, equipment records, and requests.
- **IntelliJ IDEA** – Integrated development environment.

## Project Structure

```text
LBYCPDS_Project/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── org/example/lbycpds_project/
│       │       ├── controllers/
│       │       ├── models/
│       │       ├── utils/
│       │       ├── Launcher.java
│       │       └── module-info.java
│       │
│       └── resources/
│           ├── fxmls/
│           │   ├── dashboard/
│           │   └── login/
│           ├── images/
│           └── records/
│
├── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

Before running the application, make sure you have the following installed:

- Java Development Kit (JDK)
- IntelliJ IDEA
- Maven
- JavaFX dependencies configured in the project

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/mzkakym/LBYCPDS_Project.git
   ```

2. Open IntelliJ IDEA.

3. Select **File → Open** and locate the cloned `LBYCPDS_Project` folder.

4. Allow IntelliJ IDEA to load and synchronize the Maven dependencies.

5. Locate `Launcher.java` inside:

   ```text
   src/main/java/org/example/lbycpds_project/
   ```

6. Run the `Launcher` class to start the application.

## Data Storage

The system uses a centralized local file-based storage approach to organize application information. Different records are maintained separately and linked through identifiers and related data.

These records include:

- User account information
- Equipment inventory and quantities
- Borrowing requests
- Other application-related records

This approach allows different parts of the system to access and update shared information while maintaining consistency between user accounts, equipment records, and transactions.
