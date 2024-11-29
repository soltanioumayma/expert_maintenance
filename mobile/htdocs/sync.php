<?php
// Enable error reporting
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Database connection details
$host = "localhost";
$user = "root";
$password = "";
$database = "gem";

// Establish database connection
$mysqli = new mysqli($host, $user, $password, $database);

// Check connection
if ($mysqli->connect_error) {
    die(json_encode(["status" => "error", "message" => "Database connection failed: " . $mysqli->connect_error]));
}

try {
    // Connect to SQLite (for receiving data from Android)
    $sqliteDb = new SQLite3('gem.db');

    // Fetch records with valsync = 1 from SQLite (local changes to be synced)
    $result = $sqliteDb->query("SELECT * FROM interventions WHERE valsync = 1");

    while ($row = $result->fetchArray(SQLITE3_ASSOC)) {
        // Prepare data to sync to MySQL
        $id = $row['id'];
        $terminee = $row['terminee'];  // 1 or 0
        $datePlanification = $row['dateplanification'];  // Example field

        // Update MySQL database with this intervention
        $updateQuery = "UPDATE interventions SET terminee = ?, dateplanification = ? WHERE id = ?";
        $stmt = $mysqli->prepare($updateQuery);
        $stmt->bind_param("isi", $terminee, $datePlanification, $id);
        $stmt->execute();
        
        if ($stmt->affected_rows > 0) {
            // After successful update, reset valsync to 0 in SQLite
            $sqliteDb->exec("UPDATE interventions SET valsync = 0 WHERE id = $id");
        } else {
            // Handle errors in updating
            echo json_encode(["status" => "error", "message" => "Failed to update intervention with ID: $id"]);
            exit;
        }
    }

    // Fetch records from MySQL with valsync = 1 (server changes to be synced to local)
    $result = $mysqli->query("SELECT * FROM interventions WHERE valsync = 1");

    while ($row = $result->fetch_assoc()) {
        // Check if the record exists in SQLite, if not, insert it
        $id = $row['id'];
        $terminee = $row['terminee'];
        $datePlanification = $row['dateplanification'];

        $checkQuery = "SELECT * FROM interventions WHERE id = $id";
        $checkResult = $sqliteDb->query($checkQuery);

        if ($checkResult->numRows() == 0) {
            // Insert the new record into SQLite
            $insertQuery = "INSERT INTO interventions (id, terminee, dateplanification, valsync) VALUES ($id, $terminee, '$datePlanification', 0)";
            $sqliteDb->exec($insertQuery);
        } else {
            // Update existing record in SQLite
            $updateQuery = "UPDATE interventions SET terminee = $terminee, dateplanification = '$datePlanification', valsync = 0 WHERE id = $id";
            $sqliteDb->exec($updateQuery);
        }
    }

    // Return success response after syncing both ways
    echo json_encode(["status" => "success", "message" => "Data synchronized successfully!"]);

} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
} finally {
    // Close SQLite and MySQL connections
    $sqliteDb->close();
    $mysqli->close();
}
?>
