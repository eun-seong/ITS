<!DOCTYPE html>
<html>
<head>
</head>
<body>
<?php

$id = $_GET['id'];

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

$sql = "SELECT status FROM  data WHERE id=".$id;

$result = mysqli_query($conn,$sql);

if (!$result) {
    echo "Could not successfully run query ($sql) from DB: ".mysqli_error();
    exit;
}

if (mysqli_num_rows($result) == 0) {
    echo "No rows found, nothing to print so am exiting";
    exit;
}

$row = mysqli_fetch_assoc($result);

echo "*".$row['status'];
// echo "hello world";
?>
</body>
<script type="text/javascript">
function alertUpdate(){
    window.ITS.outputUpdate();
}

window.onload = alertUpdate;
</script>
</html>
