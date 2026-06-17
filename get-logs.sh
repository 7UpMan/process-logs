#!/usr/bin/env bash
# set -x            # Enable debug
set -u              # Cause errors if variables accessed before set
set -e              # Stop as soon as there is an error
set -o pipefail     # Catch errors in pipes

# Constants we need

# Flags to control what we do
DOWNLOAD=0
IMPORT=0
REBUILD=0
CHECK=0
DELETE_OLD=0
LOAD_CRM=0
QUIET=0
LOG=0
_VERBOSE_EXPLICIT=0
VERSION=0

# Places to store and find things
DOWNLOADED_LOGS=/home/mat/server-logs

LOGFILE=/home/mat/logs/$(date +"%Y-%m-%d")-get-logs.log
PROJECT_DIR=/home/mat/Projects/process-logs
RUN_CMD=$PROJECT_DIR/run.sh
LEAD_MANAGER_DIR=/home/mat/Projects/lead-manager
LEAD_MANAGER_RUN_CMD=$LEAD_MANAGER_DIR/run.sh
CLASS_ROOT=com.s4apps.processlog

log_msg() {
	if [ ${QUIET} -eq "1" ] && [ ${LOG} -eq "0" ]; then
		return
	elif [ ${QUIET} -eq "1" ]; then
		echo "$*" >> "$LOGFILE"
	elif [ ${LOG} -eq "1" ]; then
		echo "$*" | tee -a "$LOGFILE"
	else
		echo "$*"
	fi
}

log_err() {
	echo "$*" >&2
	[ ${LOG} -eq "1" ] && echo "$*" >> "$LOGFILE"
}

run() {
	# Allows us to run a command and optionally suppress the output
	set +e
	if [ ${QUIET} -eq "1" ] && [ ${LOG} -eq "0" ]; then
		"$@" >/dev/null 2>&1
	elif [ ${QUIET} -eq "1" ]; then
		"$@" >> "$LOGFILE" 2>&1
	elif [ ${LOG} -eq "1" ]; then
		"$@" 2>&1 | tee -a "$LOGFILE"
	else
		"$@"
	fi
	EXIT_CODE=$?
	set -e
	# Check the return code and exit if there was an error
	if [ $EXIT_CODE -ne 0 ]; then
		log_err "Error running command: $*"
		exit 1
	fi
}

# Process the parameters

# If no parameters then set the default
if [ "$#" -eq 0 ]; then
	log_msg "Using default options --download --import --delete-old"
	DOWNLOAD=1
	IMPORT=1
	DELETE_OLD=1
fi

# Now iterate throught the parameters
while [ "$#" -gt 0 ]; do
	case "$1" in
		-\? | --help)
			echo "Usage: $0 [options]"
			echo "The default options if none provided are:"
			echo "	--download --import --delete-old"
			echo
			echo "Options are:"
			echo "  --download - download files (all done in this script)."
			echo "  --import - import the files (calls processlog.ProcessLog)."
			echo "  --rebuild - rebuild the ignore flags (calls processlog.Rebuild)."
			echo "  --check - check the sanity of the databse  (calls processlog.Check)."
			echo "  --delete-old - delete rolws older than 180 days old  (calls processlog.DeleteOld)."
			echo "  --load-crm - load the CRM data into ESPO."
			echo "  --version - print the version of the JAR and exit."
			echo "  --verbose - write output to screen (default, mutually exclusive with --quiet)."
			echo "  --quiet - suppress all output (mutually exclusive with --verbose)."
			echo "  --log - append output to log file (independent of --quiet/--verbose)."
			echo "  --help | -?"
			echo -e "\nNotes:"
			echo "1. If you change the ignore or delete data then all new data will be correct but"
			echo "   you need to run the rebuild to have old data updated."
			echo "2. If you remove rows from the delete data then the deleted records will be re-imported"
			echo "   as far back as the logs go.  Records that are older than the logs but < 180 days will"
			echo "   will not be re-created."
			exit 0
			;;
		--download)
			DOWNLOAD=1
			;;
		--import)
			IMPORT=1
			;;
		--rebuild)
			REBUILD=1
			;;
		--check)
			CHECK=1
			;;
		--delete-old)
			DELETE_OLD=1
			;;
		--load-crm)
			LOAD_CRM=1
			;;
		--version)
			VERSION=1
			;;
		--verbose)
			_VERBOSE_EXPLICIT=1
			;;
		--quiet)
			QUIET=1
			;;
		--log)
			LOG=1
			;;
		*)
			log_err "Invalid option $1"
			log_err "Try '$0 --help' for more information"
			exit 1
			;;
	esac
	shift
done

if [ ${QUIET} -eq "1" ] && [ ${_VERBOSE_EXPLICIT} -eq "1" ]; then
	log_err "Error: --quiet and --verbose are mutually exclusive"
	exit 1
fi

if [ ${VERSION} -eq "1" ]; then
	JAR=$(ls -t "$PROJECT_DIR"/target/*.jar 2>/dev/null | grep -vE -- '-(sources|javadoc|original|tests)\.jar$' | head -n 1 || true)
	if [ -z "$JAR" ]; then
		log_err "JAR not found. Build with: ( cd $PROJECT_DIR && mvn -q -DskipTests package )"
		exit 1
	fi
	echo "JAR: $JAR"
	java -cp "$JAR" ${CLASS_ROOT}.Version
	exit 0
fi

# Ensure the JAR is built and up to date before doing any work
JAR=$(ls -t "$PROJECT_DIR"/target/*.jar 2>/dev/null | grep -vE -- '-(sources|javadoc|original|tests)\.jar$' | head -n 1 || true)
if [ -z "$JAR" ]; then
	log_err "JAR not found. Build with: ( cd $PROJECT_DIR && mvn -q -DskipTests package )"
	exit 1
fi
if find "$PROJECT_DIR/src/main" "$PROJECT_DIR/pom.xml" -type f -newer "$JAR" | grep -q .; then
	log_err "JAR is out of date. Build with: ( cd $PROJECT_DIR && mvn -q -DskipTests package )"
	exit 1
fi

#### Get the logs
if [ ! -d "$DOWNLOADED_LOGS" ]; then
	log_err "Missing log dir of $DOWNLOADED_LOGS"
	exit 1
fi

cd ${DOWNLOADED_LOGS}

if [ ${DOWNLOAD} -eq "1" ]; then
	# We need to download the data so ...
	log_msg "************************"
	log_msg "Downloading files"

	# Clear the old files
	( rm -fr * >/dev/null )
	log_msg "Directory cleared"

	# Get the files
	log_msg "Getting new log files"
	scp u52081587@home273147721.1and1-data.host:logs/access* . > /dev/null 2>&1
	log_msg "Files downloaded"

	# Un-compress them
	log_msg "Uncompressing files"
	gunzip *gz
	log_msg "Files uncompressed"
fi

### Process all of the matching files
if [ ${IMPORT} -eq "1" ]; then
	log_msg "************************"
	log_msg "Importing files"
	run ${RUN_CMD} ${CLASS_ROOT}.ProcessLog -v --database *
fi

### Rebuild the ignore flags the new way
if [ ${REBUILD} -eq "1" ]; then
	log_msg "************************"
	log_msg "Rebuilding ignore flags"
	run ${RUN_CMD} ${CLASS_ROOT}.Rebuild
fi

### Check the database sanity
if [ ${CHECK} -eq "1" ]; then
	log_msg "************************"
	log_msg "Checking database sanity"
	run ${RUN_CMD} ${CLASS_ROOT}.Check
fi

### Delete old rows
if [ ${DELETE_OLD} -eq "1" ]; then
	log_msg "************************"
	log_msg "Deleting old rows"
	run ${RUN_CMD} ${CLASS_ROOT}.DeleteOld
fi

### Load the CRM data into ESPO
if [ ${LOAD_CRM} -eq "1" ]; then
	log_msg "************************"
	log_msg "Loading CRM data into ESPO"
	run ${LEAD_MANAGER_RUN_CMD} --info --start $( date -d "1 week ago" +"%Y-%m-%d" )
fi
