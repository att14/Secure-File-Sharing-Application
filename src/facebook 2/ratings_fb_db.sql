-- phpMyAdmin SQL Dump
-- version 3.3.10
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Apr 18, 2011 at 02:49 PM
-- Server version: 5.0.77
-- PHP Version: 5.3.5

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `cs1655_pan8`
--

-- --------------------------------------------------------

--
-- Table structure for table `appUser`
--

DROP TABLE IF EXISTS `appUser`;
CREATE TABLE IF NOT EXISTS `appUser` (
  `fb_id` bigint NOT NULL,
  `fb_username` varchar(20) NOT NULL,
  `fb_fname` varchar(30) NOT NULL,
  `fb_lname` varchar(30) NOT NULL,
  `univ_id` int(11) NOT NULL,
  PRIMARY KEY  (`fb_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `appUser`
--


-- --------------------------------------------------------

--
-- Table structure for table `class`
--

DROP TABLE IF EXISTS `class`;
CREATE TABLE IF NOT EXISTS `class` (
  `class_id` int(11) NOT NULL auto_increment,
  `course_id` int(11) NOT NULL,
  `term_id` int(11) NOT NULL,
  `p_id` int(11) NOT NULL,
  PRIMARY KEY  (`class_id`),
  UNIQUE KEY `course_id` (`course_id`,`term_id`,`p_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Dumping data for table `class`
--


-- --------------------------------------------------------

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
CREATE TABLE IF NOT EXISTS `course` (
  `course_id` int(11) NOT NULL auto_increment,
  `course_code` varchar(10) NOT NULL,
  `course_name` varchar(50) NOT NULL,
  `dept_id` int(11) NOT NULL,
  PRIMARY KEY  (`course_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=8 ;

--
-- Dumping data for table `course`
--

INSERT INTO `course` (`course_id`, `course_code`, `course_name`, `dept_id`) VALUES
(1, 'CS1501', 'Algorithm Implementation', 1),
(2, 'CS1655', 'Secure Data Management & Web Applications', 1),
(3, 'CS1555', 'Database Management Systems', 1),
(4, 'MATH1410', 'Intro to Foundations of Mathematics', 5),
(5, 'MATH1180', 'Linear Algebra 1', 5),
(6, '21-228', 'Discrete Mathematics', 6),
(7, '15-212', 'Principles of Programming', 2);

-- --------------------------------------------------------

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
CREATE TABLE IF NOT EXISTS `department` (
  `dept_id` int(11) NOT NULL auto_increment,
  `name` varchar(30) NOT NULL,
  `univ_id` int(11) NOT NULL,
  PRIMARY KEY  (`dept_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=7 ;

--
-- Dumping data for table `department`
--

INSERT INTO `department` (`dept_id`, `name`, `univ_id`) VALUES
(1, 'Computer Science', 1),
(2, 'Computer Science', 2),
(3, 'Statistics', 1),
(4, 'Physics and Astronomy', 1),
(5, 'Mathematics', 1),
(6, 'Mathematical Sciences', 2);

-- --------------------------------------------------------

--
-- Table structure for table `professor`
--

DROP TABLE IF EXISTS `professor`;
CREATE TABLE IF NOT EXISTS `professor` (
  `p_id` int(11) NOT NULL auto_increment,
  `name` varchar(40) NOT NULL,
  `url` varchar(150) NOT NULL,
  `dept_id` int(11) NOT NULL,
  PRIMARY KEY  (`p_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Dumping data for table `professor`
--


-- --------------------------------------------------------

--
-- Table structure for table `rating`
--

DROP TABLE IF EXISTS `rating`;
CREATE TABLE IF NOT EXISTS `rating` (
  `fb_id` bigint NOT NULL,
  `class_id` int(11) NOT NULL,
  `review_timestamp` datetime NOT NULL,
  `overall` int(11) NOT NULL,
  `class_easiness` int(11) NOT NULL,
  `prof_easiness` int(11) NOT NULL,
  `prof_helpfulness` int(11) NOT NULL,
  `comments` varchar(2000) default NULL,
  PRIMARY KEY  (`fb_id`,`class_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `rating`
--


-- --------------------------------------------------------

--
-- Table structure for table `term`
--

DROP TABLE IF EXISTS `term`;
CREATE TABLE IF NOT EXISTS `term` (
  `term_id` int(11) NOT NULL auto_increment,
  `season` varchar(20) NOT NULL,
  `year` int(11) NOT NULL,
  PRIMARY KEY  (`term_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=7 ;

--
-- Dumping data for table `term`
--

INSERT INTO `term` (`term_id`, `season`, `year`) VALUES
(1, 'Spring', 2011),
(2, 'Summer', 2011),
(3, 'Fall', 2010),
(4, 'Fall', 2011);

-- --------------------------------------------------------

--
-- Table structure for table `university`
--

DROP TABLE IF EXISTS `university`;
CREATE TABLE IF NOT EXISTS `university` (
  `univ_id` int(11) NOT NULL auto_increment,
  `name` varchar(40) NOT NULL,
  PRIMARY KEY  (`univ_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=3 ;

--
-- Dumping data for table `university`
--

INSERT INTO `university` (`univ_id`, `name`) VALUES
(1, 'University of Pittsburgh'),
(2, 'Carnegie Mellon University');
