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
MMT_BUILD_DIR			:= $(MMT_HOME)/build
MMT_INSTALL_RES			:= $(MMT_BUILD_DIR)/res

all: __rocksdb_install __paoding_install

clean: __rocksdb_clean

res: __res_init __paoding_res __opennlp_res

resclean:
	rm -rf $(MMT_INSTALL_RES)

__res_init:
	@mkdir -p $(MMT_INSTALL_RES)


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


