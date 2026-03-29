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

# Places to store and find things
LOG_DIR=/home/mat/server-logs

PROJECT_DIR=/home/mat/Projects/process-logs
RUN_CMD=$PROJECT_DIR/run.sh
LEAD_MANAGER_DIR=/home/mat/Projects/lead-manager
LEAD_MANAGER_RUN_CMD=$LEAD_MANAGER_DIR/run.sh
CLASS_ROOT=com.s4apps.processlog

run() {
	# Allows us to run a command and optionally suppress the output
	set +e
	if [ ${QUIET} -eq "1" ]; then
		"$@" >/dev/null 2>&1
	else
		"$@"
	fi
	EXIT_CODE=$?
	set -e
	# Check the return code and exit if there was an error
	if [ $EXIT_CODE -ne 0 ]; then
		echo "Error running command: $*"
		exit 1
	fi
}

# Process the parameters

# If no parameters then set the default
if [ "$#" -eq 0 ]; then
	echo "Using default options --download --import --delete-old"
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
			echo "  --quiet - suppress output"
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
		--quiet)
			QUIET=1
			;;
		*)
			echo "Invalid option $1"
			echo "Try '$0 --help' for more information"
			exit 1
			;;
	esac
	shift
done

#### Get the logs
if [ ! -d "$LOG_DIR" ]; then
	echo "Missing log dir of $LOG_DIR"
	exit 1
fi

cd ${LOG_DIR}

if [ ${DOWNLOAD} -eq "1" ]; then
	# We need to download the data so ...
	[ ${QUIET} -eq "0" ] && echo "************************"
	[ ${QUIET} -eq "0" ] && echo "Downloading files"

	# Clear the old files
	( rm -fr * >/dev/null )
	[ ${QUIET} -eq "0" ] && echo "Directory cleared"

	# Get the files
	[ ${QUIET} -eq "0" ] && echo "Getting new log files"
	scp u52081587@home273147721.1and1-data.host:logs/access* . > /dev/null 2>&1
	[ ${QUIET} -eq "0" ] && echo "Files downloaded"

	# Un-compress them
	[ ${QUIET} -eq "0" ] && echo "Uncompressing files"
	gunzip *gz
	[ ${QUIET} -eq "0" ] && echo "Files uncompressed"
fi

### Process all of the matching files
if [ ${IMPORT} -eq "1" ]; then
	[ ${QUIET} -eq "0" ] && echo "************************"
	[ ${QUIET} -eq "0" ] && echo "Importing files"
	run ${RUN_CMD} ${CLASS_ROOT}.ProcessLog -v --database *
fi

### Rebuild the ignore flags the new way
if [ ${REBUILD} -eq "1" ]; then
	[ ${QUIET} -eq "0" ] && echo "************************"
	[ ${QUIET} -eq "0" ] && echo "Rebuilding ignore flags"
	run ${RUN_CMD} ${CLASS_ROOT}.Rebuild
fi

### Check the database sanity
if [ ${CHECK} -eq "1" ]; then
	[ ${QUIET} -eq "0" ] && echo "************************"
	[ ${QUIET} -eq "0" ] && echo "Checking database sanity"
	run ${RUN_CMD} ${CLASS_ROOT}.Check
fi

### Delete old rows
if [ ${DELETE_OLD} -eq "1" ]; then
	[ ${QUIET} -eq "0" ] && echo "************************"
	[ ${QUIET} -eq "0" ] && echo "Deleting old rows"
	run ${RUN_CMD} ${CLASS_ROOT}.DeleteOld
fi

### Load the CRM data into ESPO
if [ ${LOAD_CRM} -eq "1" ]; then
	[ ${QUIET} -eq "0" ] && echo "************************"
	[ ${QUIET} -eq "0" ] && echo "Loading CRM data into ESPO"
	${LEAD_MANAGER_RUN_CMD} --info --start $( date -d "1 week ago" +"%Y-%m-%d" ) >> ~mat/tmp/load-crm.log 2>&1
fi
