/*
 Navicat PostgreSQL Data Transfer

 Source Server         : localhost
 Source Server Version : 90104
 Source Host           : localhost
 Source Database       : abm
 Source Schema         : public

 Target Server Version : 90104
 File Encoding         : utf-8

 Date: 11/05/2013 10:27:23 AM
*/

-- ----------------------------
--  Table structure for dot
-- ----------------------------
DROP TABLE IF EXISTS "dot";
CREATE TABLE "dot" (
	"gid" int4 NOT NULL DEFAULT nextval('dot_gid_seq'::regclass),
	"agentid" int4,
	"tick" int4,
	"geom" "geometry",
	"id" int4,
	"roadid" varchar(10) COLLATE "default"
)
WITH (OIDS=FALSE);
ALTER TABLE "dot" OWNER TO "postgres";

-- ----------------------------
--  Primary key structure for table dot
-- ----------------------------
ALTER TABLE "dot" ADD CONSTRAINT "dot_pkey" PRIMARY KEY ("gid") NOT DEFERRABLE INITIALLY IMMEDIATE;

