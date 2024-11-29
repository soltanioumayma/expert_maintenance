<?php
header('Content-Type: application/json');

// Database connection
$host = "localhost";
$user = "root";
$password = "";
$database = "gem";

$mysqli = new mysqli($host, $user, $password, $database);

if ($mysqli->connect_error) {
    die(json_encode(["status" => "error", "message" => $mysqli->connect_error]));
}

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

if (!isset($input['images']) || !is_array($input['images'])) {
    die(json_encode(["status" => "error", "message" => "Invalid input"]));
}

try {
    foreach ($input['images'] as $image) {
        $name = $mysqli->real_escape_string($image['nom']);
        $imgData = base64_decode($image['img']);
        $dateCapture = $mysqli->real_escape_string($image['dateCapture']);
        $interventionId = intval($image['intervention_id']);

        $stmt = $mysqli->prepare("INSERT INTO images (nom, img, dateCapture, intervention_id, valsync) VALUES (?, ?, ?, ?, 0)");
        $stmt->bind_param("sbsi", $name, $imgData, $dateCapture, $interventionId);
        $stmt->send_long_data(1, $imgData); // Send BLOB data
        $stmt->execute();
        $stmt->close();
    }

    echo json_encode(["status" => "success", "message" => "Images uploaded successfully"]);
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
} finally {
    $mysqli->close();
}
?>
