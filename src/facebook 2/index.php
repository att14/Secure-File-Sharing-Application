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

	$code = $_REQUEST["code"];
	
	if(empty($code)) {
		$dialog_url = "https://www.facebook.com/dialog/oauth?client_id=" 
			. $app_id . "&redirect_uri=" . urlencode($my_url);
		
		echo("<script> top.location.href='" . $dialog_url . "'</script>");
	}

  	$token_url = "https://graph.facebook.com/oauth/access_token?client_id="		
 		. $app_id . "&redirect_uri=" . urlencode($my_url) . "&client_secret="
 		. $app_secret . "&code=" . $code;
	$_SESSION['token'] = $token_url;
 	$access_token = file_get_contents($token_url);
 	$graph_url = "https://graph.facebook.com/me?" . $access_token;
 	$user = json_decode(file_get_contents($graph_url));

	if(isset($_POST['overall_rating'])) {
		$db->query('INSERT INTO rating (fb_id, class_id, review_timestamp, overall, class_easiness, prof_easiness, prof_helpfulness, comments) VALUES (' . $_SESSION['id'] . ', ' . $_SESSION['class_id'] . ', FROM_UNIXTIME(' . time() . '), ' . $_POST['overall_rating'] . ', ' . $_POST['class_easiness'] . ', ' . $_POST['professor_easiness'] . ', ' . $_POST['professor_helpfulness'] . ', "' . $_POST['comment'] . '")'); 
	}
	$_SESSION['id'] = $user->id;
	$_SESSION['name'] = $user->name;
	$_SESSION['first_name'] = $user->first_name;
	$_SESSION['last_name'] = $user->last_name;
	$_SESSION['username'] = $user->username;
 
 	if (mysqli_connect_error()) { 
     	die('Connect Error (' . mysqli_connect_errno() . ') ' 
             . mysqli_connect_error()); 
 	}

	if ($_SESSION['no_professor'] == 1) {
		echo 'There are currently no professors listed for that selection.';
		echo '<br />';
		$_SESSION['no_professor'] = 0;
	}

	echo 'Welcome, ' . $_SESSION['first_name'] . ' ' . $_SESSION['last_name'];

	echo '<form action=university.php method="post" name="rate_class">';
	echo '<input type="submit" name="sub_rate" value="Rate Class">';
	echo '</form>';

	echo '<form action=select_prof_univ.php method="post" name="view_prof_class">';
	echo '<input type="submit" name="sub_rate" value="View Professor">';
	echo '</form>';

	echo '<form action=select_course_univ.php method="post" name="view_course_class">';
	echo '<input type="submit" name="sub_rate" value="View Course">';
	echo '</form>';
?>
