<?php
// Enable error reporting
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Clear previous output and set JSON header
ob_clean();
header('Content-Type: application/json');

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
    // Query the images table
    $result = $mysqli->query("SELECT * FROM images");
    if (!$result) {
        throw new Exception("Error fetching images: " . $mysqli->error);
    }

    // Fetch all images as an associative array
    $images = $result->fetch_all(MYSQLI_ASSOC);

    // Return the images as JSON
    echo json_encode(["status" => "success", "data" => $images]);
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
} finally {
    // Close the database connection
    $mysqli->close();
}
?>
