<?php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$password = "";
$database = "gem";
$response = array();

try {
    // Ensure the request method is POST
    if ($_SERVER['REQUEST_METHOD'] == 'POST') {
        // Read and decode the input JSON
        $input = json_decode(file_get_contents("php://input"), true);

        if (isset($input['id'])) {
            $interventionId = $input['id'];

            // Establish database connection
            $mysqli = new mysqli($host, $user, $password, $database);

            // Check for connection errors
            if ($mysqli->connect_error) {
                throw new Exception("Database connection failed: " . $mysqli->connect_error);
            }

            // Prepare the DELETE query
            $stmt = $mysqli->prepare("DELETE FROM interventions WHERE id = ?");
            if (!$stmt) {
                throw new Exception("Failed to prepare statement: " . $mysqli->error);
            }

            // Bind the parameter
            $stmt->bind_param("i", $interventionId);

            // Execute the query and check for errors
            if ($stmt->execute()) {
                if ($stmt->affected_rows > 0) {
                    $response['status'] = 'success';
                    $response['message'] = 'Intervention deleted successfully';
                } else {
                    $response['status'] = 'error';
                    $response['message'] = 'No intervention found with the provided ID';
                }
            } else {
                throw new Exception("Failed to execute query: " . $stmt->error);
            }

            // Close the statement and connection
            $stmt->close();
            $mysqli->close();
        } else {
            $response['status'] = 'error';
            $response['message'] = 'Invalid input: ID is missing';
        }
    } else {
        $response['status'] = 'error';
        $response['message'] = 'Invalid request method';
    }
} catch (Exception $e) {
    $response['status'] = 'error';
    $response['message'] = 'Exception occurred: ' . $e->getMessage();
}

// Output the JSON response
echo json_encode($response);
?>
