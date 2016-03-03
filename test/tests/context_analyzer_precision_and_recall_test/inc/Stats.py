import json


class Stats:

    def __init__(self):
        self.__number_of_matches = 0
        self.__sum_position = 0
        self.__sum_score_gap = 0
        self.__number_of_found = 0
        self.__number_of_queries = 0
        self.__total_time = 0

    def add_sample(self, results, domain_id, query_time):
        self.__number_of_queries += 1
        match = False
        position = -1
        gap = -1
        found = False
        if len(results) > 0:
            match = (results[0]['id'] == domain_id)
            for idx, result in enumerate(results):
                if result['id'] == domain_id:
                    position = idx + 1
                    if idx + 1 < len(results):
                        gap = result['score'] - results[idx + 1]['score']
                    else:
                        gap = result['score']
                    found = True
                    break
        self.add(match, position, gap, found, query_time)

    def add(self, match, position, gap, found, query_time):
        if match:
            self.__number_of_matches += 1
        if found:
            self.__number_of_found += 1
            self.__sum_position += 1
            self.__sum_score_gap += gap
        self.__total_time += query_time

    def get_stats(self):
        float_number_of_queries = float(self.__number_of_queries)
        float_number_of_found = float(self.__number_of_found)
        stats = {
            'Precision': float('nan') if float_number_of_queries == 0 else self.__number_of_matches / float_number_of_queries,
            'Recall':  float('nan') if float_number_of_queries == 0 else self.__number_of_found / float_number_of_queries,
            'Average position':  float('nan') if float_number_of_found == 0 else self.__sum_position / float_number_of_found,
            'Average score gap':  float('nan') if float_number_of_found == 0 else self.__sum_score_gap / float_number_of_found,
            'Average query time [s]':  float('nan') if float_number_of_queries == 0 else self.__total_time / float_number_of_queries
        }
        return  stats

    def __str__(self):
        return json.dumps(self.get_stats(), indent=4, separators=(',', ': '))