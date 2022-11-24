.DEFAULT_GOAL := list

ANT_PATH := "${PWD}/lib/ant/apache-ant-1.10.12/bin/ant"
ANT := "$(shell if [[ -x "${ANT_PATH}" ]]; then echo "${ANT_PATH}"; else echo ""; fi)"

IVY_PATH := "${PWD}/lib/ivy/apache-ivy-2.5.1/ivy-2.5.1.jar"
IVY := "$(shell if [[ -f "${IVY_PATH}" ]]; then echo "${IVY_PATH}" else echo ""; fi)"

ifeq ("${ANT}", "")
ifeq ("${IVY}", "")
.PHONY: init
init: init-ant init-ivy
endif
endif

ifeq ("${ANT}","")
.PHONY: init-ant
init-ant:
	@ [[ -d "lib" ]] || mkdir "lib";
	@echo "download ant"
	@curl --request GET -sL \
		--url "https://dlcdn.apache.org//ant/binaries/apache-ant-1.10.12-bin.zip" \
		--output "lib/ant-bin.zip"
	@unzip "lib/ant-bin.zip" -d "lib/ant"
endif

ifeq ("${IVY}","")
.PHONY: init-ivy
init-ivy:
	@ [[ -d "lib" ]] || mkdir "lib";
	@echo "download ivy"
	@curl --request GET -sL \
	     --url "https://dlcdn.apache.org//ant/ivy/2.5.1/apache-ivy-2.5.1-bin.zip"\
	     --output 'lib/ivy-bin.zip'
	@unzip "lib/ivy-bin.zip" -d "lib/ivy"
endif

ifeq ("${PROJECT}", "")

.PHONY: new
new:
	@echo "new"
	@echo "no PROJECT defined"

.PHONY: run
run:
	@echo "run"
	@echo "no PROJECT defined"

.PHONY: clean
clean:
	@echo "clean"
	@echo "no PROJECT defined"

else # if PROJECT

.PHONY: new
new:
	@echo "new"
	@echo "ant=${ANT}"
	@echo "ivy=${IVY}"
	@echo "project=${PROJECT}"
	@mkdir -p "${PROJECT}/src/com/example"
	@mkdir -p "${PROJECT}/resources"
	@./scripts/build-xml.sh "${PROJECT}"
	@./scripts/ivy-xml.sh "${PROJECT}"

.PHONY: resolve
resolve:
	@echo "resolve"
	@echo "ant=${ANT}"
	@echo "ivy=${IVY}"
	@echo "project=${PROJECT}"
	@cd "${PROJECT}"; "${ANT}" -lib "${IVY}" resolve

.PHONY: idea
idea:
	@echo "resolve"
	@echo "ant=${ANT}"
	@echo "ivy=${IVY}"
	@echo "project=${PROJECT}"
	@./scripts/mk-iml.sh "${PROJECT}"

.PHONY: compile
compile:
	@echo "compile"
	@echo "ant=${ANT}"
	@echo "ivy=${IVY}"
	@echo "project=${PROJECT}"
	@cd "${PROJECT}"; "${ANT}" -lib "${IVY}" compile

.PHONY: run
run:
	@echo "run"
	@echo "ant=${ANT}"
	@echo "ivy=${IVY}"
	@echo "project=${PROJECT}"
	@cd "${PROJECT}"; "${ANT}" -lib "${IVY}" run

.PHONY: clean
clean:
	@echo "clean"
	@echo "ant=${ANT}"
	@echo "ivy=${IVY}"
	@echo "project=${PROJECT}"
	@cd "${PROJECT}"; "${ANT}" -lib "${IVY}" clean

.PHONY: clean-deps
clean-deps:
	@echo "clean"
	@echo "ant=${ANT}"
	@echo "ivy=${IVY}"
	@echo "project=${PROJECT}"
	@rm -rf "${PROJECT}/build/libs"


endif # if PROJECT

REQUEST_SEC := "$(shell if [[ -z "${REQUEST_SEC}" ]]; then echo "120" ; else echo "${REQUEST_SEC}"; fi)"
DURATION := "$(shell if [[ -z "${DURATION}" ]]; then echo "60" ; else echo "${DURATION}"; fi)"
TOTAL := "$(shell echo "${REQUEST_SEC} * ${DURATION}" | bc)"
ABC := "$(shell which "ab")"
REQUEST_ID := "$(shell ./scripts/request-id.sh)"

.PHONY: test
test:
	@echo "Running test"
	@echo "parameters:"
	@echo "  request/sec: ${REQUEST_SEC}"
	@echo "  duration   : ${DURATION} sec"
	@echo "  total req  : ${TOTAL}"
	@echo "command: ${ABC}"
	@echo "header: ${REQUEST_ID}"
	@"${ABC}" \
			-n "${TOTAL}" \
			-c "${REQUEST_SEC}" \
			-t "${DURATION}" \
			-s 5 \
			-H "X-ID:${REQUEST_ID}" \
			http://localhost:8080/api

.PHONY: list
list:
	@make -f Makefile -p |\
		grep -B 1 "Phony target" |\
		grep -v "Phony target" |\
		grep -v "commands to execute" |\
		grep ":" |\
		tr -d ':'
