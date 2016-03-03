import json, os, requests, socket, subprocess, sys, time
from inc.Stats import Stats
from optparse import OptionParser

class ContextAnalyzerPrecisionAndRecallTest:

    MIN_PRECISION = 0.7
    DEFAULT_CONTEXT_LINES_COUNT = 30
    DEFAULT_NUMBER_OF_RESULTS = 10
    ENGINE_NAME = 'context_analyzer_test_engine'
    SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))
    MMT_ROOT_DIR = os.path.dirname(
        os.path.dirname(
            os.path.dirname(SCRIPT_DIR)))
    DEV_PATH = os.path.join(os.path.join(os.path.join(MMT_ROOT_DIR,"engines/"), ENGINE_NAME), "data/test/")

    def __init__(self, language_code, training_directory, test_directory, context_lines, results, verbosity_level):
        self.__language_code = language_code
        self.__training_directory = training_directory
        self.__test_directory = test_directory
        self.__context_lines = context_lines
        self.__results = results
        self.__verbosity_level = verbosity_level

    def start_test(self):
        try:
            port = self.get_free_port()
            self.start_engine(port)
            stats = self.query_context_analyzer(port)
            passed = self.check_results(stats)
            json_response = {"passed": passed, "results": stats.get_stats()}
        except Exception as e:
            json_response = {"passed": False, "error": str(e)}
        finally:
            self.stop_engine()
        print json.dumps(json_response)

    def start_engine(self, port):
        self.log("Moving into: " + ContextAnalyzerPrecisionAndRecallTest.MMT_ROOT_DIR)
        os.chdir(ContextAnalyzerPrecisionAndRecallTest.MMT_ROOT_DIR)

        self.log("Creating engine...")
        process = subprocess.Popen(['./mmt', 'create', '-e', ContextAnalyzerPrecisionAndRecallTest.ENGINE_NAME, 'en',
                                    'it', self.__training_directory , '-s', 'preprocess', 'context_analyzer'],
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = process.communicate()
        self.log(out)
        self.log(err)
        self.log("Starting engine (port: " + str(port) + ")...")
        process = subprocess.Popen(['./mmt', 'start', '-e', ContextAnalyzerPrecisionAndRecallTest.ENGINE_NAME, '-p',
                                    str(port), '--no-slave'],
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = process.communicate()
        self.log(out)
        self.log(err)
        self.log("Moving into: " + ContextAnalyzerPrecisionAndRecallTest.SCRIPT_DIR)
        os.chdir(ContextAnalyzerPrecisionAndRecallTest.SCRIPT_DIR)

    def query_context_analyzer(self, port):
        stats = Stats()
        self.log("Starting test")
        for (domain_id, file_path) in self.get_corpora():
            self.log("Testing: " + domain_id)
            for context in self.get_context(file_path):
                start_time = time.time()
                headers = {'content-type': 'text/plain'}
                json_results = requests.get("http://localhost:"+str(port)+"/context",
                                 data = context,
                                 headers=headers)
                results = json.loads(json_results.text)
                query_time = time.time() - start_time
                stats.add_sample(results, domain_id, query_time)
        return stats

    def stop_engine(self):
        self.log("Moving into: " + ContextAnalyzerPrecisionAndRecallTest.MMT_ROOT_DIR)
        os.chdir(ContextAnalyzerPrecisionAndRecallTest.MMT_ROOT_DIR)

        self.log("Stopping engine...")
        process = subprocess.Popen(['./mmt', 'stop', '-e', ContextAnalyzerPrecisionAndRecallTest.ENGINE_NAME],
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = process.communicate()
        self.log(out)
        self.log(err)
        self.log("Moving into: " + ContextAnalyzerPrecisionAndRecallTest.SCRIPT_DIR)
        os.chdir(ContextAnalyzerPrecisionAndRecallTest.SCRIPT_DIR)

    def check_results(self, stats):
        return stats.get_stats()['Precision'] >= ContextAnalyzerPrecisionAndRecallTest.MIN_PRECISION

    def get_corpora(self):
        extension = "." + self.__language_code
        for test in os.listdir(self.__test_directory):
            if test.endswith(extension):
                yield (test[:-len(extension)], os.path.join(self.__test_directory, test))

    def get_context(self, file_path):
        context_lines = []
        with open(file_path, 'rU') as f:
            read = True
            while read:
                if len(context_lines) > 0:
                    context_lines.pop(0)
                while len(context_lines) < self.__context_lines:
                    current_line = f.readline().strip()
                    if current_line == '':
                        read = False
                        break
                    context_lines.append(current_line)
                yield "\n".join(context_lines)

    def get_free_port(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(("",0))
        port = s.getsockname()[1]
        s.close()
        return port

    def log(self, message):
        if int(self.__verbosity_level) > 0:
            print message


if __name__ == "__main__":
    parser = OptionParser()
    parser.add_option("-l", "--lang-code", dest="language_code", metavar='Language', default="it",
                      help="the language code to use as filter for the test set")
    parser.add_option("--training-dir", dest="training_directory", metavar='Training_directory',
              default=os.path.join(ContextAnalyzerPrecisionAndRecallTest.MMT_ROOT_DIR, "examples/data/train/"),
              help="the path of the directory that contains the training set")
    parser.add_option("--test-dir", dest="test_directory", metavar='Test_directory',
              default= ContextAnalyzerPrecisionAndRecallTest.DEV_PATH,
              help="the path of the directory that contains the test set")
    parser.add_option("--context-lines", dest="context_lines", metavar='Context_line', type=int,
                      default=ContextAnalyzerPrecisionAndRecallTest.DEFAULT_CONTEXT_LINES_COUNT,
                      help="the number of contiguos lines to use to build the context string")
    parser.add_option("-r", "--results", dest="results", metavar='Number_of_results', type=int,
                      default=ContextAnalyzerPrecisionAndRecallTest.DEFAULT_NUMBER_OF_RESULTS,
                      help="the number of documents to retrieve from the context analyzer by a single query")
    parser.add_option("-v", "--verbosity-level", dest="verbosity_level",
              default="0",
              help="the verbosity level: should be 0=default,1")

    options, _ = parser.parse_args()
    analyzer = ContextAnalyzerPrecisionAndRecallTest(**vars(options))
    analyzer.start_test()