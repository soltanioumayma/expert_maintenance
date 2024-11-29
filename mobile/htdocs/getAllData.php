<?php
// Enable error reporting
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Clear any previous output and set JSON header
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

$data = [];
try {
    // Query clients
    $clientsResult = $mysqli->query("SELECT * FROM clients");
    if (!$clientsResult) {
        throw new Exception("Error fetching clients: " . $mysqli->error);
    }

    // Query sites
    $sitesResult = $mysqli->query("SELECT * FROM sites");
    if (!$sitesResult) {
        throw new Exception("Error fetching sites: " . $mysqli->error);
    }

    // Query interventions with related client_id
    $interventionsResult = $mysqli->query("
        SELECT i.*, s.client_id
        FROM interventions i
        JOIN sites s ON i.site_id = s.id
    ");
    if (!$interventionsResult) {
        throw new Exception("Error fetching interventions: " . $mysqli->error);
    }

    // Query contrats
    $contratsResult = $mysqli->query("SELECT * FROM contrats");
    if (!$contratsResult) {
        throw new Exception("Error fetching contrats: " . $mysqli->error);
    }

    // Query employes
    $employesResult = $mysqli->query("SELECT * FROM employes");
    if (!$employesResult) {
        throw new Exception("Error fetching employes: " . $mysqli->error);
    }

    // Query priorites
    $prioritesResult = $mysqli->query("SELECT * FROM priorites");
    if (!$prioritesResult) {
        throw new Exception("Error fetching priorites: " . $mysqli->error);
    }

    // Query taches
    $tachesResult = $mysqli->query("SELECT * FROM taches");
    if (!$tachesResult) {
        throw new Exception("Error fetching taches: " . $mysqli->error);
    }

    

    // Query employes_interventions
    $employesInterventionsResult = $mysqli->query("SELECT * FROM employes_interventions");
    if (!$employesInterventionsResult) {
        throw new Exception("Error fetching employes_interventions: " . $mysqli->error);
    }

    // Populate results into arrays
    $data["clients"] = $clientsResult->fetch_all(MYSQLI_ASSOC);
    $data["sites"] = $sitesResult->fetch_all(MYSQLI_ASSOC);
    $data["interventions"] = $interventionsResult->fetch_all(MYSQLI_ASSOC);
    $data["contrats"] = $contratsResult->fetch_all(MYSQLI_ASSOC);
    $data["employes"] = $employesResult->fetch_all(MYSQLI_ASSOC);
    $data["priorites"] = $prioritesResult->fetch_all(MYSQLI_ASSOC);
    $data["taches"] = $tachesResult->fetch_all(MYSQLI_ASSOC);
    $data["employes_interventions"] = $employesInterventionsResult->fetch_all(MYSQLI_ASSOC);

    // Return success response
    echo json_encode(["status" => "success", "tables" => $data]);
} catch (Exception $e) {
    // Handle exceptions and return error response
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
} finally {
    // Close the database connection
    $mysqli->close();
}
?>