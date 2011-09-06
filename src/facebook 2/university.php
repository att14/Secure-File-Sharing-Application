<?php	
	session_start();

	$dbhost="db7.cs.pitt.edu"; 
	$dbuser="cs1655_att14"; 
	$dbpass="4203"; //last four digits of your peopleSoft id 
	$dbname = "cs1655_att14"; 
	$db = new mysqli($dbhost, $dbuser, $dbpass, $dbname); 
	$app_id = "203470623020222";
	$app_secret = "2cbdee1c819f5f06ee95dd5255b327f5";
	$my_url = "http://cs1520.cs.pitt.edu/~att14/php/";
	
	$res = $db->prepare("SELECT univ_id FROM appUser WHERE fb_id=" . $_SESSION['id']);
	$res->execute();
	$res->bind_result($univ_id);

	if($res->fetch()) {
		$_SESSION['univ_id'] = $univ_id;
		header("Location: department.php");
	}

	$res = $db->prepare("SELECT name, univ_id FROM university"); 
 	$res->execute();
 	$res->bind_result($univ_name, $univ_id);
 	
 	$univ_name_arr = array();
 	$univ_id_arr = array();
 	
 	while($res->fetch()) {
 		$univ_name_arr[] = $univ_name;
 		$univ_id_arr[] = $univ_id;
 	}
 	
 	createDropdown($univ_name_arr, 'univ_name', 'University: ', 'department');
	 		
 	function createDropdown($arr, $frm, $label, $action) {
 		echo $label;
 		echo '<form action="' . $action . '.php" method="post" name="' . $frm . '">';
 		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">'; 		
 		foreach ($arr as $key => $value) {
 			echo '<option value="'.$value.'">'.$value.'</option>';
 		}
 		echo '</select>';
		echo '<INPUT TYPE = "Submit" Name = "sub_school" VALUE = "Submit">';
 		echo '</form>';
 	}
?>
