ANT_BIN_PATH := "lib/ant/apache-ant-1.10.12/bin"
ANT :=  "$$(if [[ -d "${ANT_BIN_PATH}" ]]; then echo "${ANT_BIN_PATH}/ant"; else echo ""; fi)"
IVY_PATH := "lib/ant/apache-ivy-2.5.1"
IVY := "$$(if [[ -d "${IVY_PATH}" ]]; then echo "${IVY_PATH}/ivy-2.5.1.jar"; else echo ""; fi)"

ifeq ("${ANT}", "")
.PHONY: init
init: init-ant init-ivy

.PHONY: init-ant
init-ant:
	@ [[ -d "lib" ]] || mkdir "lib";
	@echo "download ant"
	@curl --request GET -sL \
		--url "https://dlcdn.apache.org//ant/binaries/apache-ant-1.10.12-bin.zip" \
		--output "lib/ant-bin.zip"
	@unzip "lib/ant-bin.zip" -d "lib/ant"

.PHONY: init-ivy
init-ivy:
	@ [[ -d "lib" ]] || mkdir "lib";
	@echo "download ivy"
	@curl --request GET -sL \
	     --url "https://dlcdn.apache.org//ant/ivy/2.5.1/apache-ivy-2.5.1-bin.zip"\
	     --output 'lib/ivy-bin.zip'
	@unzip "lib/ivy-bin.zip" -d "lib/ivy"
endif

.PHONY: test
test:
	echo "test"

ifeq ("${PROJECT}", "")

.PHONY: run
run:
	@echo "run"
	@echo "no PROJECT defined"

.PHONY: clean
clean:
	@echo "clean"
	@echo "no PROJECT defined"

else # if PROJECT

PROJECT_DIR := "$$(if [[ -d "${PROJECT}" ]]; then echo "${PROJECT}"; else echo ""; fi)"

ifneq ("${PROJECT_DIR}", "${PROJECT}")

.PHONY: run
run:
	@echo "run"
	@echo "directory '${PROJECT}' not found"

.PHONY: clean
clean:
	@echo "clean"
	@echo "directory '${PROJECT}' not found"

else  # if PROJECT_DIR

.PHONY: run
run:
	@echo "run"
	@echo "directory '${PROJECT_DIR}'"

.PHONY: clean
clean:
	@echo "clean"
	@echo "directory '${PROJECT_DIR}'"

endif # if PROJECT_DIR
endif # if PROJECT

.PHONY: help
help:
	@echo "targets"
	@echo "    init : initializes ant/ivy"
	@echo "    run  : runs application"
	@echo "           - PROJECT: (env variable) the name of application"
	@echo "    clean: cleans application artifact"
	@echo "           - PROJECT: (env variable) the name of application"
