NPROCS		:= 1
OS		:= $(shell uname -s)

ifeq ($(OS),Linux)
	NPROCS := $(shell grep -c ^processor /proc/cpuinfo)
endif
ifeq ($(OS),Darwin) # Assume Mac OS X
	NPROCS := $(shell sysctl -n hw.ncpu)
endif

# MMT Paths
VENDOR_DIR				:= $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

MMT_HOME				:= $(realpath $(VENDOR_DIR)/..)
MMT_NATIVE_INCLUDE_DIR	:= $(MMT_HOME)/src/common-interfaces/native/include
MMT_BUILD_DIR			:= $(MMT_HOME)/build
MMT_INSTALL_RES			:= $(MMT_BUILD_DIR)/res

all: __moses_install __rocksdb_install __rockslm_install __sapt_install __paoding_install

clean: __moses_clean __rocksdb_clean __rockslm_clean __sapt_clean

res: __res_init __paoding_res __opennlp_res

resclean:
	rm -rf $(MMT_INSTALL_RES)

__res_init:
	@mkdir -p $(MMT_INSTALL_RES)


# ==========================================
#  MODULE: Moses decoder
# ==========================================

MOSES_HOME		:= $(VENDOR_DIR)/moses
MOSES_BUILD_DIR	:= $(MOSES_HOME)/build
	
__moses_install: __moses
	@echo "Installing Moses..."
	@cd $(MOSES_BUILD_DIR); make install
	@echo "SUCCESS (Moses)"

__moses: __rockslm_install __sapt_install
	@echo "Building Moses..."
	@mkdir -p $(MOSES_BUILD_DIR)
	@cd $(MOSES_BUILD_DIR); cmake $(MOSES_HOME) -DMMT_ROOT=$(MMT_HOME) -DUSE_SAPT=1 -DUSE_ROCKSLM=1 -DROCKSDB_ROOT=$(MMT_BUILD_DIR) -DROCKSLM_ROOT=$(MMT_BUILD_DIR) -DSAPT_ROOT=$(MMT_BUILD_DIR) -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=$(MMT_BUILD_DIR) -Wno-dev
	@cd $(MOSES_BUILD_DIR); make -j$(NPROCS)
	@echo "SUCCESS (Moses)"

__moses_clean:
	rm -rf $(MOSES_BUILD_DIR)

# ==========================================
#  MODULE: SAPT (under MMT)
# ==========================================

SAPT_HOME    := $(MMT_HOME)/src/suffixarray-phrasetable

__sapt_install: __sapt
	@echo "Installing SAPT..."
	@cd $(SAPT_HOME); mvn install
	@echo "SUCCESS (SAPT)"

__sapt: __rocksdb_install
	@echo "Building SAPT..."
	@cd $(ROCKSLM_HOME); mvn verify
	@echo "SUCCESS (SAPT)"

__sapt_clean:
	@cd $(SAPT_HOME); mvn clean


# ==========================================
#  MODULE: RocksLM (under MMT)
# ==========================================

ROCKSLM_HOME	:= $(MMT_HOME)/src/rockslm

__rockslm_install: __rockslm
	@echo "Installing RocksLM..."
	@cd $(ROCKSLM_HOME); mvn install
	@echo "SUCCESS (RocksLM)"

__rockslm: __rocksdb_install
	@echo "Building RocksLM..."
	@cd $(ROCKSLM_HOME); mvn verify
	@echo "SUCCESS (RocksLM)"

__rockslm_clean:
	@cd $(ROCKSLM_HOME); mvn clean


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


# ==========================================
#  MODULE: RocksDB
# ==========================================

ROCKSDB_HOME		:= $(VENDOR_DIR)/rocksdb

__rocksdb_install: __rocksdb
	@echo "Installing RocksDB..."
	@cd $(ROCKSDB_HOME); INSTALL_PATH=$(MMT_BUILD_DIR) make install-shared
	@echo "SUCCESS (RocksDB)"

__rocksdb: 
	@echo "Building RocksDB..."
	@cd $(ROCKSDB_HOME); make -j$(NPROCS) shared_lib
	@echo "SUCCESS (RocksDB)"

__rocksdb_clean:
	@cd $(ROCKSDB_HOME); make clean


