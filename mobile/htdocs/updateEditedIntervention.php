<?php
header('Content-Type: application/json');
$host = "localhost";
$user = "root";
$password = "";
$database = "gem";

$response = ["status" => "error"];

try {
    $mysqli = new mysqli($host, $user, $password, $database);
    if ($mysqli->connect_error) {
        throw new Exception("Database connection failed: " . $mysqli->connect_error);
    }

    $input = json_decode(file_get_contents('php://input'), true);
    if (!isset($input['id'])) {
        throw new Exception("Invalid input data");
    }

    $stmt = $mysqli->prepare("UPDATE interventions SET titre = ?, heuredebuteffect = ?, heurefineffect = ?, dateplanification = ?, priorite_id = ? WHERE id = ?");
    $stmt->bind_param("ssssii", $input['titre'], $input['heuredebuteffect'], $input['heurefineffect'], $input['dateplanification'], $input['priorite_id'], $input['id']);
    if ($stmt->execute()) {
        $response["status"] = "success";
    } else {
        throw new Exception("Failed to update: " . $stmt->error);
    }
} catch (Exception $e) {
    $response["message"] = $e->getMessage();
}

echo json_encode($response);
?>
