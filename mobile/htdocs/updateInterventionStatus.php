<?php
header('Content-Type: application/json');

// Database connection details
$host = "localhost";
$user = "root";
$password = "";
$database = "gem";

$response = ["status" => "error"];

try {
    // Establish database connection
    $mysqli = new mysqli($host, $user, $password, $database);
    if ($mysqli->connect_error) {
        throw new Exception("Database connection failed: " . $mysqli->connect_error);
    }

    // Get the input data
    $input = json_decode(file_get_contents('php://input'), true);
    if (!isset($input['id']) || !isset($input['terminee'])) {
        throw new Exception("Invalid input");
    }

    // Prepare and execute the update query
    $stmt = $mysqli->prepare("UPDATE interventions SET terminee = ? WHERE id = ?");
    $stmt->bind_param("ii", $input['terminee'], $input['id']);
    if ($stmt->execute()) {
        $response["status"] = "success";
    } else {
        throw new Exception("Failed to update intervention: " . $stmt->error);
    }
} catch (Exception $e) {
    $response["message"] = $e->getMessage();
} finally {
    echo json_encode($response);
    if (isset($mysqli)) {
        $mysqli->close();
    }
}
?>
