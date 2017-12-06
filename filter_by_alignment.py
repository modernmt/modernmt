import os
import sys
import math

def main(argv):
    if len(argv) < 3:
        raise Exception("Missing parameter")

    path_in=argv[1]
    path_out=argv[2]
    threshold = float(argv[3]) if len(argv) > 3 else 3.0

    sys.stderr.write("path_in:%s path_out:%s thr:%s\n" % (path_in, path_out, threshold))

    with open(os.path.join(path_in,"train.score")) as score_stream:

        scores = {}
        line = 0
        while (True):
            score_line = score_stream.readline()
            if not score_line:
                break

            score=score_line.strip().split()
            s = float(score[0])
            if math.isinf(s) or math.isnan(s):
                scores[line] = -1000.0
            else:
                scores[line] = s
            line += 1

        selected_scores, z_scores = select(scores, threshold)


    #print selected sentence pairs
    with open(os.path.join(path_in,"train.src"),"r") as source_stream:
        with open(os.path.join(path_in,"train.trg"),"r") as target_stream:
            with open(os.path.join(path_out,"train_filtered.src"),"w") as source_filtered_stream:
                with open(os.path.join(path_out,"train_filtered.trg"),"w") as target_filtered_stream:
                    with open(os.path.join(path_out,"train_filtered.score"),"w") as score_filtered_stream:
                        with open(os.path.join(path_out,"train_removed.src"),"w") as source_removed_stream:
                            with open(os.path.join(path_out,"train_removed.trg"),"w") as target_removed_stream:
                                with open(os.path.join(path_out,"train_removed.score"),"w") as score_removed_stream:
                                    with open(os.path.join(path_out,"train.label"),"w") as output_stream:
                                        line = 0
                                        selected = 0
                                        while (True):
                                            source_line = source_stream.readline()
                                            target_line = target_stream.readline()
                                            if not source_line or not target_line:
                                                break

                                            if selected_scores[line] == True:
                                                source_filtered_stream.write(source_line)
                                                target_filtered_stream.write(target_line)
                                                output_stream.write("1\n")  # TMOP value for BAD
                                                if scores[line] > -1000.0:
                                                    score_filtered_stream.write("%f %f\n" % (scores[line], z_scores[line]))
                                                else:
                                                    score_filtered_stream.write("MY_NAN %f\n" % (z_scores[line]))
                                                selected += 1
                                            else:
                                                source_removed_stream.write(source_line)
                                                target_removed_stream.write(target_line)
                                                output_stream.write("3\n")  # TMOP value for BAD
                                                if scores[line] > -1000.0:
                                                    score_removed_stream.write("%f %f\n" % (scores[line], z_scores[line]))
                                                else:
                                                    score_removed_stream.write("MY_NAN %f\n" % (z_scores[line]))

                                            line += 1

    sys.stderr.write("%s selected among %s sentence pairs\n" % (selected, len(scores)))


def select(scores,threshold):
    selected_scores, z_scores = {}, {}
    for c in scores:
        if (scores[c] > -1000.0) and (scores[c] > threshold):
            selected_scores[c] = True
        else:
            selected_scores[c] = False
        z_scores[c] = scores[c]


    return selected_scores, z_scores

def select2(scores,threshold):
    sum, squared_sum = 0.0, 0.0
    N = len(scores)
    for c in scores:
        sum += scores[c] # sum all scores
    mean  = sum / N
    N_squared_sum = 0

    for c in scores:
        if (scores[c] < mean):
            squared_sum += (mean - scores[c])
            N_squared_sum += 1

    std_dev = squared_sum / N_squared_sum
    sys.stderr.write("N:%s sum:%s squared_sum:%s mean:%s std_dev:%s\n" % (N, sum, squared_sum, mean, std_dev))

    selected_scores, z_scores = {}, {}
    for c in scores:
        z = ( mean - scores[c] ) / std_dev
        if (scores[c] > 0.0) and (z < threshold):
            # sys.stderr.write("c:%s scores[c]:%s z:%s thr:%s KEPT\n" % (c, scores[c], z, threshold))
            selected_scores[c] = True
        else:
            # sys.stderr.write("c:%s scores[c]:%s z:%s thr:%s SKIP\n" % (c, scores[c], z, threshold))
            selected_scores[c] = False
        z_scores[c] = z


    return selected_scores, z_scores

if __name__ == '__main__':
    main(sys.argv)
