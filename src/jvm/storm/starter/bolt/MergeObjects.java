package storm.starter.bolt;

import org.apache.log4j.Logger;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MergeObjects extends BaseBasicBolt {
    public static Logger LOG = Logger.getLogger(MergeObjects.class);

    private List<List> _rankings = new ArrayList();
    int _count = 10;
    Long _lastTime;

    public MergeObjects(int n) {
        _count = n;
    }

    private int _compare(List one, List two) {
        long valueOne = (Long) one.get(1);
        long valueTwo = (Long) two.get(1);
        long delta = valueTwo - valueOne;
        if(delta > 0) {
            return 1;
        } else if (delta < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    private Integer _find(Object tag) {
        for(int i = 0; i < _rankings.size(); ++i) {
            Object cur = _rankings.get(i).get(0);
            if (cur.equals(tag)) {
                return i;
            }
        }
        return null;
    }

    public void execute(Tuple tuple, BasicOutputCollector collector) {
        List<List> merging = (List) tuple.getValue(0);
        for(List pair : merging) {
            Integer existingIndex = _find(pair.get(0));
            if (null != existingIndex) {
                _rankings.set(existingIndex, pair);
            } else {
                _rankings.add(pair);
            }

            Collections.sort(_rankings, new Comparator<List>() {
                public int compare(List o1, List o2) {
                    return _compare(o1, o2);
                }
            });

            if (_rankings.size() > _count) {
                _rankings.subList(_count, _rankings.size()).clear();
            }
        }

        long currentTime = System.currentTimeMillis();
        if(_lastTime==null || currentTime >= _lastTime + 2000) {
            collector.emit(new Values(new ArrayList(_rankings)));
            LOG.info("Rankings: " + _rankings);
            _lastTime = currentTime;
        }
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("list"));
    }
}
