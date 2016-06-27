NPROCS		:= 1
OS			:= $(shell uname -s)

ifeq ($(OS),Linux)
	NPROCS := $(shell grep -c ^processor /proc/cpuinfo)
endif
ifeq ($(OS),Darwin) # Assume Mac OS X
	NPROCS := $(shell sysctl -n hw.ncpu)
endif

# MMT Paths
VENDOR_DIR		:= $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

MMT_HOME		:= $(realpath $(VENDOR_DIR)/..)
MMT_BUILD_DIR	:= $(MMT_HOME)/build
MMT_INSTALL_LIB	:= $(MMT_BUILD_DIR)/lib
MMT_INSTALL_BIN	:= $(MMT_BUILD_DIR)/bin
MMT_INSTALL_RES	:= $(MMT_BUILD_DIR)/res

all: __moses __fastalign

install: __install_init __moses_install __fastalign_install __paoding_install

clean: __moses_clean __fastalign_clean

res: __res_init __paoding_res __opennlp_res

resclean:
	rm -rf $(MMT_INSTALL_RES)

__install_init:
	@mkdir -p $(MMT_INSTALL_LIB)
	@mkdir -p $(MMT_INSTALL_BIN)

__res_init:
	@mkdir -p $(MMT_INSTALL_RES)

# ==========================================
#  MODULE: Moses decoder
# ==========================================

MOSES_HOME		:= $(VENDOR_DIR)/moses
MOSES_BUILD_DIR	:= $(MOSES_HOME)/build
MOSES_LIB_DIR	:= $(MOSES_BUILD_DIR)/lib
MOSES_BIN_DIR	:= $(MOSES_BUILD_DIR)/bin

MOSES_INSTALL	:= $(MMT_INSTALL_BIN)/moses
MERT_INSTALL	:= $(MMT_INSTALL_BIN)/mert
KENLM_INSTALL	:= $(MMT_INSTALL_BIN)/kenlm
MOSES_LIBS		:= libmoses libmertc 
MOSES_EXES		:= mmlex-build mtt-build symal symal2mam
MERT_EXES		:= evaluator extractor kbmira mert pro
KENLM_EXES		:= lmplz build_binary

__moses: 
	@echo "Building Moses..."
	@mkdir -p $(MOSES_BUILD_DIR)
	@cd $(MOSES_BUILD_DIR); cmake $(MOSES_HOME) -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$(MOSES_BUILD_DIR)" -Wno-dev
	@cd $(MOSES_BUILD_DIR); make -j$(NPROCS)
	@echo "SUCCESS (Moses)"
	
__moses_install:
	@echo "Installing Moses..."
	@mkdir -p $(MOSES_INSTALL)
	@mkdir -p $(MERT_INSTALL)
	@mkdir -p $(KENLM_INSTALL)
	$(foreach exe,$(MOSES_EXES),cp $(MOSES_BIN_DIR)/$(exe) $(MOSES_INSTALL)/$(exe);)
	$(foreach exe,$(MERT_EXES),cp $(MOSES_BIN_DIR)/$(exe) $(MERT_INSTALL)/$(exe);)
	$(foreach exe,$(KENLM_EXES),cp $(MOSES_BIN_DIR)/$(exe) $(KENLM_INSTALL)/$(exe);)
	$(foreach lib,$(MOSES_LIBS),cp -r $(MOSES_LIB_DIR)/$(lib)* $(MMT_INSTALL_LIB)/;)
	@echo "SUCCESS (Moses)"

__moses_clean:
	rm -rf $(MOSES_BUILD_DIR)
	rm -rf $(MOSES_INSTALL)
	rm -rf $(MERT_INSTALL)
	rm -rf $(KENLM_INSTALL)
	rm -f $(foreach lib,$(MOSES_LIBS),$(MMT_INSTALL_LIB)/$(lib)*)


# ==========================================
#  MODULE: FastAlign
# ==========================================

FA_HOME			:= $(VENDOR_DIR)/fastalign
FA_BUILD_DIR	:= $(FA_HOME)/build
FA_LIB_DIR		:= $(FA_BUILD_DIR)/lib
FA_BIN_DIR		:= $(FA_BUILD_DIR)/bin

FA_INSTALL		:= $(MMT_INSTALL_BIN)/fastalign
FA_EXES			:= fast_align
FA_LIBS			:= libfastalign

__fastalign: 
	@echo "Building FastAlign..."
	@mkdir -p $(FA_BUILD_DIR)
	@cd $(FA_BUILD_DIR); cmake $(FA_HOME) -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$(FA_BUILD_DIR)" -Wno-dev
	@cd $(FA_BUILD_DIR); make -j$(NPROCS)
	@echo "SUCCESS (FastAlign)"
	
__fastalign_install:
	@echo "Installing FastAlign..."
	@mkdir -p $(FA_INSTALL)
	$(foreach exe,$(FA_EXES),cp $(FA_BIN_DIR)/$(exe) $(FA_INSTALL)/$(exe);)
	$(foreach lib,$(FA_LIBS),cp -r $(FA_LIB_DIR)/$(lib)* $(MMT_INSTALL_LIB)/;)
	@echo "SUCCESS (FastAlign)"

__fastalign_clean:
	rm -rf $(FA_BUILD_DIR)
	rm -rf $(FA_INSTALL)
	rm -f $(foreach lib,$(FA_LIBS),$(MMT_INSTALL_LIB)/$(lib)*)


# ==========================================
#  MODULE: Paoding Analysis
# ==========================================

PAODING_HOME	:= $(VENDOR_DIR)/paoding
PAODING_RES		:= $(MMT_INSTALL_RES)/paoding

__paoding_install:
	mvn install:install-file -Dfile=$(PAODING_HOME)/paoding-analysis.jar -DpomFile=$(PAODING_HOME)/paoding-analysis.pom

__paoding_res:
	@mkdir -p $(PAODING_RES)
	tar -xzf $(PAODING_HOME)/dictionary.tar.gz -C $(PAODING_RES)/


# ==========================================
#  MODULE: OpenNLP
# ==========================================

OPENNLP_HOME	:= $(VENDOR_DIR)/opennlp
OPENNLP_RES		:= $(MMT_INSTALL_RES)/opennlp

__opennlp_res:
	@mkdir -p $(OPENNLP_RES)
	cp $(OPENNLP_HOME)/*.bin $(OPENNLP_RES)/
















