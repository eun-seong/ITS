<!DOCTYPE html>
<html>
<head>
</head>
<body>
<?php

$id = $_GET['id'];
$status = $_GET['status'];

$conn = mysqli_connect("localhost", "EUN", "DMSTJDWl12~");
mysqli_query($conn,'SET NAMES utf8');

if (!$conn) {
    echo "Unable to connect to DB: " . mysqli_error();
    exit;
}

if (!mysqli_select_db($conn, "its_db")) {
    echo "Unable to select mydbname: " . mysqli_error();
    exit;
}

$sql = "UPDATE data SET status={$status} WHERE id={$id}";
mysqli_query($conn,$sql);

$sql = "SELECT *
        FROM  data
    LIMIT 2";

$result = mysqli_query($conn,$sql);

if (!$result) {
    echo "Could not successfully run query ($sql) from DB: " . mysqli_error();
    exit;
}


if (mysqli_num_rows($result) == 0) {
    echo "No rows found, nothing to print so am exiting";
    exit;
}

// While a row of data exists, put that row in $row as an associative array
// Note: If you're expecting just one row, no need to use a loop
// Note: If you put extract($row); inside the following loop, you'll
//       then create $userid, $fullname, and $userstatus

?>
</body>
<script type="text/javascript">
function alertUpdate(){
    window.ITS.outputUpdate();
}
window.onload = alertUpdate;
</script>
</html>
