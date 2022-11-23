ANT := "${PWD}/lib/ant/apache-ant-1.10.12/bin/ant"
ANT := "$(shell if [[ -x "${ANT}" ]]; then echo "${ANT}"; else echo ""; fi)"

IVY_PATH := "lib/ivy/apache-ivy-2.5.1"
IVY := "${PWD}/lib/ivy/apache-ivy-2.5.1/ivy-2.5.1.jar"

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

.PHONY: help
help:
	@echo "targets"
	@echo "    init : initializes ant/ivy"
	@echo "    run  : runs application"
	@echo "           - PROJECT: (env variable) the name of application"
	@echo "    clean: cleans application artifact"
	@echo "           - PROJECT: (env variable) the name of application"
