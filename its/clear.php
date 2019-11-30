<!DOCTYPE html>
<html>
<head>
</head>
<body>
<?php
$conn = mysqli_connect("localhost", "EUN", "DMSTJDWl12~");

if (!$conn) {
    echo "Unable to connect to DB: " . mysqli_error();
    exit;
}

if (!mysqli_select_db($conn, "its_db")) {
    echo "Unable to select mydbname: " . mysqli_error();
    exit;
}

$sql = "UPDATE data SET status=0";

mysqli_query($conn,$sql);

$sql = "SELECT * FROM  data LIMIT 2";

$result = mysqli_query($conn, $sql);

if (!$result) {
    echo "Could not successfully run query ($sql) from DB: " . mysqli_error();
    exit;
}

// While a row of data exists, put that row in $row as an associative array
// Note: If you're expecting just one row, no need to use a loop
// Note: If you put extract($row); inside the following loop, you'll
//       then create $userid, $fullname, and $userstatus
echo "<table>";
while ($row = mysqli_fetch_assoc($result)) {
    echo "<tr><td>{$row['id']}</td><td><p id=\"id{$row['id']}\">{$row['status']}</p></td></tr>";
}
echo "</table>";
mysqli_free_result($result);

?>
</body>
<script type="text/javascript">
function alertUpdate(){
    window.ITS.outputUpdate();
}

window.onload = alertUpdate;
</script>
</html>
