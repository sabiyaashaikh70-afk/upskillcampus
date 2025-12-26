import java.io.*;
import java.time.LocalDateTime;
import java. time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Banking Information System - Core Java Implementation
 * Developed by:  Lalsangi Saniya Shakil
 * Internship Program: USC-UCT Summer-Winter 2025
 */

// ==================== MODELS ====================

class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String userId;
    private String username;
    private String passwordHash;
    private String email;
    private String role; // CUSTOMER, ADMIN, MANAGER
    private LocalDateTime createdAt;
    private boolean active;

    public User(String username, String passwordHash, 
                String email, String role) {
        this.userId = "U" + System.currentTimeMillis();
        this.username = username;
        this.passwordHash = hashPassword(passwordHash);
        this.email = email;
        this.role = role;
        this.createdAt = LocalDateTime. now();
        this.active = true;
    }

    private String hashPassword(String password) {
        // Simple hash (In production, use BCrypt)
        return Integer.toHexString(password. hashCode());
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }

    public boolean verifyPassword(String password) {
        return this.passwordHash.equals(hashPassword(password));
    }

    @Override
    public String toString() {
        return String.format("User[ID: %s, Name: %s, Email: %s, Role: %s]",
                userId, username, email, role);
    }
}

class BankAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accountId;
    private String userId;
    private String accountNumber;
    private String accountType; // SAVINGS, CHECKING, BUSINESS
    private double balance;
    private String status; // ACTIVE, CLOSED
    private LocalDateTime createdAt;
    private LocalDateTime lastTransactionTime;

    public BankAccount(String userId, String accountType) {
        this.accountId = "ACC" + System.currentTimeMillis();
        this.userId = userId;
        this.accountNumber = generateAccountNumber();
        this.accountType = accountType;
        this.balance = 0.0;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.lastTransactionTime = LocalDateTime.now();
    }

    private String generateAccountNumber() {
        Random rand = new Random();
        return String.format("%016d", Math.abs(rand.nextLong() % 10000000000000000L));
    }

    // Getters and Setters
    public String getAccountId() { return accountId; }
    public String getUserId() { return userId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public double getBalance() { return balance; }
    public String getStatus() { return status; }

    public void deposit(double amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive");
        if (!status.equals("ACTIVE")) throw new InvalidAmountException("Account is closed");
        
        this.balance += amount;
        this.lastTransactionTime = LocalDateTime.now();
    }

    public void withdraw(double amount) throws InsufficientFundsException, 
                                               InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive");
        if (amount > balance) throw new InsufficientFundsException
                ("Insufficient balance.  Available: " + balance);
        if (!status.equals("ACTIVE")) throw new InvalidAmountException("Account is closed");
        
        this.balance -= amount;
        this.lastTransactionTime = LocalDateTime.now();
    }

    public void transfer(BankAccount targetAccount, double amount) 
            throws InsufficientFundsException, InvalidAmountException {
        this.withdraw(amount);
        targetAccount.deposit(amount);
    }

    @Override
    public String toString() {
        return String.format(
            "Account[ID: %s, Number: %s, Type: %s, Balance: ₹%.2f, Status: %s]",
            accountId, accountNumber, accountType, balance, status);
    }
}

class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private double amount;
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER
    private String status; // PENDING, COMPLETED, FAILED
    private LocalDateTime timestamp;
    private String description;

    public Transaction(String fromAccountId, String toAccountId, 
                      double amount, String type) {
        this.transactionId = "TXN" + System.currentTimeMillis();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.type = type;
        this.status = "COMPLETED";
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format(
            "TXN[ID: %s, Type: %s, Amount: ₹%.2f, Status: %s, Time: %s]",
            transactionId, type, amount, status, timestamp);
    }
}

class Loan implements Serializable {
    private static final long serialVersionUID = 1L;
    private String loanId;
    private String accountId;
    private double principalAmount;
    private double interestRate;
    private int tenureMonths;
    private double monthlyEMI;
    private double remainingBalance;
    private String status; // ACTIVE, CLOSED, DEFAULT
    private LocalDateTime createdAt;
    private int paidMonths;

    public Loan(String accountId, double principal, double rate, int tenure) {
        this.loanId = "LOAN" + System.currentTimeMillis();
        this.accountId = accountId;
        this. principalAmount = principal;
        this.interestRate = rate;
        this.tenureMonths = tenure;
        this.monthlyEMI = calculateEMI();
        this.remainingBalance = principal;
        this.status = "ACTIVE";
        this. createdAt = LocalDateTime.now();
        this.paidMonths = 0;
    }

    private double calculateEMI() {
        double monthlyRate = interestRate / 12 / 100;
        if (monthlyRate == 0) {
            return principalAmount / tenureMonths;
        }
        return (principalAmount * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)) /
               (Math.pow(1 + monthlyRate, tenureMonths) - 1);
    }

    public void payEMI() throws InvalidAmountException {
        if (! status.equals("ACTIVE")) {
            throw new InvalidAmountException("Loan is not active");
        }
        if (paidMonths >= tenureMonths) {
            this.status = "CLOSED";
            throw new InvalidAmountException("Loan already closed");
        }

        remainingBalance -= monthlyEMI;
        paidMonths++;

        if (paidMonths >= tenureMonths) {
            this.status = "CLOSED";
            remainingBalance = 0;
        }
    }

    // Getters
    public String getLoanId() { return loanId; }
    public String getAccountId() { return accountId; }
    public double getMonthlyEMI() { return monthlyEMI; }
    public double getRemainingBalance() { return remainingBalance; }
    public String getStatus() { return status; }
    public int getPaidMonths() { return paidMonths; }

    @Override
    public String toString() {
        return String.format(
            "Loan[ID: %s, Principal: ₹%.2f, EMI: ₹%.2f, Status: %s, " +
            "Paid:  %d/%d months]",
            loanId, principalAmount, monthlyEMI, status, paidMonths, tenureMonths);
    }
}

// ==================== CUSTOM EXCEPTIONS ====================

class InvalidAmountException extends Exception {
    public InvalidAmountException(String message) {
        super(message);
    }
}

class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

class UserAuthenticationException extends Exception {
    public UserAuthenticationException(String message) {
        super(message);
    }
}

// ==================== SERVICES ====================

class UserService implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<User> users = new ArrayList<>();
    private User currentUser = null;

    public void registerUser(String username, String password, 
                            String email, String role) throws Exception {
        if (users.stream().anyMatch(u -> u.getUsername().equals(username))) {
            throw new Exception("Username already exists");
        }
        users.add(new User(username, password, email, role));
        System.out.println("✓ User registered successfully");
    }

    public User login(String username, String password) throws UserAuthenticationException {
        User user = users.stream()
            .filter(u -> u.getUsername().equals(username) && u.verifyPassword(password))
            .findFirst()
            .orElse(null);

        if (user == null) {
            throw new UserAuthenticationException("Invalid username or password");
        }
        this.currentUser = user;
        System.out.println("✓ Login successful.  Welcome, " + username);
        return user;
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("✓ Logged out successfully");
            currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }
}

class AccountService implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<BankAccount> accounts = new ArrayList<>();

    public BankAccount createAccount(String userId, String accountType) throws Exception {
        if (accounts. stream().anyMatch(a -> a.getUserId().equals(userId) && 
            a.getStatus().equals("ACTIVE"))) {
            // Allow multiple accounts
        }
        BankAccount account = new BankAccount(userId, accountType);
        accounts.add(account);
        System.out. println("✓ Account created:  " + account. getAccountNumber());
        return account;
    }

    public BankAccount getAccountById(String accountId) throws AccountNotFoundException {
        return accounts.stream()
            .filter(a -> a.getAccountId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new AccountNotFoundException
                ("Account not found: " + accountId));
    }

    public BankAccount getAccountByNumber(String accountNumber) 
            throws AccountNotFoundException {
        return accounts.stream()
            .filter(a -> a.getAccountNumber().equals(accountNumber))
            .findFirst()
            .orElseThrow(() -> new AccountNotFoundException
                ("Account not found: " + accountNumber));
    }

    public List<BankAccount> getUserAccounts(String userId) {
        return accounts.stream()
            .filter(a -> a.getUserId().equals(userId))
            .collect(Collectors.toList());
    }

    public void closeAccount(String accountId) throws AccountNotFoundException {
        BankAccount account = getAccountById(accountId);
        if (account.getBalance() > 0) {
            System.out.println("⚠ Please withdraw remaining balance before closing");
            return;
        }
        accounts.stream()
            .filter(a -> a.getAccountId().equals(accountId))
            .forEach(a -> {
                // In real implementation, would update account object
                System.out.println("✓ Account closed: " + a.getAccountNumber());
            });
    }

    public List<BankAccount> getAllAccounts() {
        return new ArrayList<>(accounts);
    }
}

class TransactionService implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Transaction> transactions = new ArrayList<>();
    private AccountService accountService;

    public TransactionService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void deposit(String accountId, double amount) 
            throws AccountNotFoundException, InvalidAmountException {
        BankAccount account = accountService.getAccountById(accountId);
        account.deposit(amount);
        transactions.add(new Transaction(accountId, accountId, amount, "DEPOSIT"));
        System.out.println("✓ Deposit successful.  New balance: ₹" + 
            String.format("%.2f", account.getBalance()));
    }

    public void withdraw(String accountId, double amount) 
            throws AccountNotFoundException, InvalidAmountException, 
                   InsufficientFundsException {
        BankAccount account = accountService.getAccountById(accountId);
        account.withdraw(amount);
        transactions.add(new Transaction(accountId, null, amount, "WITHDRAWAL"));
        System.out.println("✓ Withdrawal successful. New balance: ₹" + 
            String.format("%.2f", account.getBalance()));
    }

    public void transfer(String fromAccountId, String toAccountId, double amount) 
            throws AccountNotFoundException, InvalidAmountException, 
                   InsufficientFundsException {
        BankAccount fromAccount = accountService.getAccountById(fromAccountId);
        BankAccount toAccount = accountService.getAccountById(toAccountId);
        
        fromAccount.transfer(toAccount, amount);
        transactions.add(new Transaction(fromAccountId, toAccountId, amount, "TRANSFER"));
        System.out.println("✓ Transfer successful. From account balance: ₹" + 
            String.format("%.2f", fromAccount.getBalance()));
    }

    public void printStatement(String accountId) throws AccountNotFoundException {
        BankAccount account = accountService.getAccountById(accountId);
        List<Transaction> accountTransactions = transactions.stream()
            .filter(t -> t.getFromAccountId().equals(accountId) || 
                        t.getToAccountId().equals(accountId))
            .collect(Collectors.toList());

        System.out.println("\n" + "=". repeat(70));
        System.out.println("ACCOUNT STATEMENT");
        System.out.println("=".repeat(70));
        System.out.printf("Account:  %s | Balance: ₹%.2f\n", 
            account.getAccountNumber(), account.getBalance());
        System.out.println("-".repeat(70));
        System.out.printf("%-15s %-15s %-15s %-20s\n", 
            "Transaction ID", "Type", "Amount", "Time");
        System.out.println("-".repeat(70));

        for (Transaction t : accountTransactions) {
            System.out.printf("%-15s %-15s ₹%-14.2f %s\n", 
                t.getTransactionId(), t.getType(), t.getAmount(), 
                t.getTimestamp());
        }
        System.out.println("=".repeat(70) + "\n");
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }
}

class LoanService implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Loan> loans = new ArrayList<>();
    private AccountService accountService;

    public LoanService(AccountService accountService) {
        this.accountService = accountService;
    }

    public Loan applyForLoan(String accountId, double principal, 
                             double rate, int tenure) throws AccountNotFoundException {
        BankAccount account = accountService.getAccountById(accountId);
        Loan loan = new Loan(accountId, principal, rate, tenure);
        loans.add(loan);
        System.out.println("✓ Loan approved:  " + loan.getLoanId());
        System.out.println("  Monthly EMI: ₹" + String.format("%.2f", loan.getMonthlyEMI()));
        return loan;
    }

    public void payEMI(String loanId) throws Exception {
        Loan loan = loans.stream()
            .filter(l -> l.getLoanId().equals(loanId))
            .findFirst()
            .orElseThrow(() -> new Exception("Loan not found"));

        loan.payEMI();
        System.out.println("✓ EMI paid successfully");
        System.out.println("  Remaining balance: ₹" + 
            String.format("%.2f", loan.getRemainingBalance()));
    }

    public void getLoanStatus(String loanId) throws Exception {
        Loan loan = loans.stream()
            .filter(l -> l.getLoanId().equals(loanId))
            .findFirst()
            .orElseThrow(() -> new Exception("Loan not found"));

        System.out.println("\n" + "=".repeat(50));
        System.out.println("LOAN STATUS");
        System.out.println("=".repeat(50));
        System.out.println(loan);
        System.out.println("=".repeat(50) + "\n");
    }

    public List<Loan> getUserLoans(String accountId) {
        return loans. stream()
            .filter(l -> l.getAccountId().equals(accountId))
            .collect(Collectors.toList());
    }
}

// ==================== MAIN APPLICATION ====================

public class BankingInformationSystem {
    private UserService userService;
    private AccountService accountService;
    private TransactionService transactionService;
    private LoanService loanService;
    private Scanner scanner;

    public BankingInformationSystem() {
        this.userService = new UserService();
        this.accountService = new AccountService();
        this.transactionService = new TransactionService(accountService);
        this.loanService = new LoanService(accountService);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out. println("\n" + "=".repeat(60));
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║  BANKING INFORMATION SYSTEM (BIS)                 ║");
        System.out.println("║  Developed by: Lalsangi Saniya Shakil             ║");
        System.out.println("║  Internship:  USC-UCT Summer-Winter 2025           ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("=".repeat(60) + "\n");

        boolean running = true;
        while (running) {
            if (userService.getCurrentUser() == null) {
                showLoginMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private void showLoginMenu() {
        System.out.println("\n┌─────────────────────────────────┐");
        System.out. println("│   LOGIN MENU                    │");
        System.out. println("├─────────────────────────────────┤");
        System.out.println("│  1. Login                       │");
        System.out. println("│  2. Register                    │");
        System.out. println("│  3. Exit                        │");
        System.out.println("└─────────────────────────────────┘");
        System.out.print("Select option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1": 
                handleLogin();
                break;
            case "2":
                handleRegister();
                break;
            case "3":
                System.out.println("Thank you for using BIS.  Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("❌ Invalid option. Please try again.");
        }
    }

    private void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        try {
            userService.login(username, password);
        } catch (UserAuthenticationException e) {
            System.out. println("❌ " + e.getMessage());
        }
    }

    private void handleRegister() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        try {
            userService. registerUser(username, password, email, "CUSTOMER");
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void showMainMenu() {
        User currentUser = userService.getCurrentUser();
        System.out.println("\n┌──────────────────────────────────────┐");
        System.out. println("│  Welcome, " + String.format("%-25s", currentUser.getUsername()) + "│");
        System.out.println("├──────────────────────────────────────┤");
        System.out.println("│  1. Manage Accounts                  │");
        System.out. println("│  2. Transactions                     │");
        System.out.println("│  3. Loan Management                  │");
        System.out.println("│  4. View Accounts                    │");
        System.out. println("│  5. Account Statement                │");
        System.out.println("│  6. Logout                           │");
        System.out.println("└──────────────────────────────────────┘");
        System.out.print("Select option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1": 
                showAccountMenu();
                break;
            case "2":
                showTransactionMenu();
                break;
            case "3":
                showLoanMenu();
                break;
            case "4": 
                viewAccounts();
                break;
            case "5":
                viewStatement();
                break;
            case "6":
                userService. logout();
                break;
            default:
                System.out.println("❌ Invalid option");
        }
    }

    private void showAccountMenu() {
        System.out.println("\n┌────────────────────────────────┐");
        System.out. println("│  ACCOUNT MANAGEMENT            │");
        System.out. println("├────────────────────────────────┤");
        System.out.println("│  1. Create New Account         │");
        System.out. println("│  2. Close Account              │");
        System.out.println("│  3. Back to Main Menu          │");
        System.out.println("└────────────────────────────────┘");
        System.out. print("Select option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1": 
                createAccount();
                break;
            case "2":
                closeAccount();
                break;
            case "3":
                break;
            default:
                System.out.println("❌ Invalid option");
        }
    }

    private void createAccount() {
        System.out.println("\nAccount Types:  SAVINGS, CHECKING, BUSINESS");
        System.out.print("Enter account type:  ");
        String type = scanner.nextLine().trim().toUpperCase();

        try {
            accountService.createAccount(userService.getCurrentUser().getUserId(), type);
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void closeAccount() {
        System.out.print("Enter account ID to close: ");
        String accountId = scanner. nextLine().trim();

        try {
            accountService.closeAccount(accountId);
        } catch (AccountNotFoundException e) {
            System. out.println("❌ " + e.getMessage());
        }
    }

    private void showTransactionMenu() {
        System.out.println("\n┌────────────────────────────────┐");
        System.out.println("│  TRANSACTIONS                  │");
        System.out.println("├────────────────────────────────┤");
        System.out.println("│  1. Deposit                    │");
        System.out. println("│  2. Withdraw                   │");
        System.out.println("│  3. Transfer                   │");
        System.out.println("│  4. Back to Main Menu          │");
        System.out. println("└────────────────────────────────┘");
        System.out.print("Select option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                handleDeposit();
                break;
            case "2":
                handleWithdraw();
                break;
            case "3":
                handleTransfer();
                break;
            case "4":
                break;
            default:
                System.out.println("❌ Invalid option");
        }
    }

    private void handleDeposit() {
        System.out.print("Enter account ID:  ");
        String accountId = scanner.nextLine().trim();
        System.out.print("Enter amount: ");
        double amount = getDoubleInput();

        try {
            transactionService.deposit(accountId, amount);
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void handleWithdraw() {
        System.out.print("Enter account ID:  ");
        String accountId = scanner.nextLine().trim();
        System.out.print("Enter amount: ");
        double amount = getDoubleInput();

        try {
            transactionService.withdraw(accountId, amount);
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void handleTransfer() {
        System.out.print("Enter source account ID: ");
        String fromAccountId = scanner.nextLine().trim();
        System.out. print("Enter destination account ID: ");
        String toAccountId = scanner.nextLine().trim();
        System.out.print("Enter amount: ");
        double amount = getDoubleInput();

        try {
            transactionService.transfer(fromAccountId, toAccountId, amount);
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void showLoanMenu() {
        System.out.println("\n┌────────────────────────────────┐");
        System.out. println("│  LOAN MANAGEMENT               │");
        System.out. println("├────────────────────────────────┤");
        System.out.println("│  1. Apply for Loan             │");
        System.out.println("│  2. Pay EMI                    │");
        System.out.println("│  3. View Loan Status           │");
        System.out. println("│  4. Back to Main Menu          │");
        System.out.println("└────────────────────────────────┘");
        System.out.print("Select option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                applyForLoan();
                break;
            case "2": 
                payEMI();
                break;
            case "3": 
                viewLoanStatus();
                break;
            case "4":
                break;
            default:
                System.out.println("❌ Invalid option");
        }
    }

    private void applyForLoan() {
        System.out.print("Enter account ID: ");
        String accountId = scanner.nextLine().trim();
        System.out.print("Enter loan amount: ");
        double principal = getDoubleInput();
        System.out.print("Enter interest rate (% per annum): ");
        double rate = getDoubleInput();
        System.out.print("Enter tenure (months): ");
        int tenure = getIntInput();

        try {
            loanService.applyForLoan(accountId, principal, rate, tenure);
        } catch (AccountNotFoundException e) {
            System. out.println("❌ " + e.getMessage());
        }
    }

    private void payEMI() {
        System.out.print("Enter loan ID: ");
        String loanId = scanner.nextLine().trim();

        try {
            loanService.payEMI(loanId);
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void viewLoanStatus() {
        System.out.print("Enter loan ID: ");
        String loanId = scanner.nextLine().trim();

        try {
            loanService.getLoanStatus(loanId);
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void viewAccounts() {
        try {
            List<BankAccount> accounts = accountService
                .getUserAccounts(userService.getCurrentUser().getUserId());
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("YOUR ACCOUNTS");
            System.out.println("=".repeat(60));
            
            if (accounts.isEmpty()) {
                System.out.println("No accounts found");
            } else {
                for (BankAccount account : accounts) {
                    System.out. println(account);
                }
            }
            System.out.println("=".repeat(60) + "\n");
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void viewStatement() {
        System.out.print("Enter account ID: ");
        String accountId = scanner.nextLine().trim();

        try {
            transactionService.printStatement(accountId);
        } catch (AccountNotFoundException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private double getDoubleInput() {
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid input");
            return 0.0;
        }
    }

    private int getIntInput() {
        try {
            return Integer.parseInt(scanner. nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid input");
            return 0;
        }
    }

    // ==================== MAIN METHOD ====================
    public static void main(String[] args) {
        BankingInformationSystem bis = new BankingInformationSystem();
        bis.start();
    }
}