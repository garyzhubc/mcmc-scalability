package matchings;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import bayonet.distributions.Multinomial;
import bayonet.distributions.Random;
import blang.core.LogScaleFactor;
import blang.distributions.Generators;
import blang.mcmc.ConnectedFactor;
import blang.mcmc.SampledVariable;
import blang.mcmc.Sampler;

/**
 * Each time a Permutation is encountered in a Blang model, 
 * this sampler will be instantiated. 
 */
public class BipartiteMatchingSampler implements Sampler {
  /**
   * This field will be populated automatically with the 
   * permutation being sampled. 
   */
  @SampledVariable BipartiteMatching matching;
  /**
   * This will contain all the elements of the prior or likelihood 
   * (collectively, factors), that depend on the permutation being 
   * resampled. 
   */
  @ConnectedFactor List<LogScaleFactor> numericFactors;

  @Override
  public void execute(Random rand) {
	  // empty matching - add one edge [-1 -1 -1] -> [2 -1 -1] : choose i uniformly then j uniformly, set i-th entry to j
	  // partial matching - add or delete one edge (or swap?) [2 3 -1] -> [-1 3 -1] or [-1 2 -1]: add or delete uniformly
	  // full matching - delete one edge (or swap?) [0 2 1] -> [-1 2 1]: choose i uniformly and set to -1
	  
	  // make copy
	  double log_prob_o = logDensity();
	  List<Integer> conn_o = new ArrayList<Integer>(matching.getConnections());
	  
	  // set quantities
	  int n = matching.componentSize();
	  int k = matching.free1().size();
	  int m = n-k;
	  double log_prob_otn,log_prob_nto;
	  int i,j,l,q,s;
	  
	  // propose
	  if (k!=0) {
		  i = rand.nextInt(k*k+m);
		  if (i<=m-1) {
			  j = getUnfree1().get(i);
			  matching.getConnections().set(j,BipartiteMatching.FREE);
			  log_prob_otn = -Math.log(Math.pow(k,2)+m);
			  log_prob_nto = -Math.log(Math.pow(k+1,2)+m-1); 
		  } else {
			  i = i-m;
			  s = i/k;
			  j = i%k;
			  l = matching.free1().get(s);
			  q = matching.free2().get(j);
			  matching.getConnections().set(l,q);
			  log_prob_otn = -Math.log(Math.pow(k,2)+m);
			  log_prob_nto = -Math.log(Math.pow(k-1,2)+m+1); 
		  }
	  } else {
		  i = rand.nextInt(n);
		  matching.getConnections().set(i,BipartiteMatching.FREE);
		  log_prob_otn = 0; 
		  log_prob_nto = 0; 
	  }
	  
	  // accept or reject
	  double log_prob_n = logDensity();
	  double alpha = Math.min(1,Math.exp(log_prob_n-log_prob_o+log_prob_nto-log_prob_otn));
	  boolean d = rand.nextBernoulli(alpha);
	  if (!d) {
		  // if don't accept, restore old connections
		  matching.getConnections().clear();matching.getConnections().addAll(conn_o);
	  } 
  }
  
  private List<Integer> getUnfree1() {
	  // get linked vertices in the first component
	  List<Integer> unfr1 = new ArrayList<Integer>();
	  for (int p=0;p<matching.componentSize();p++) {
		  if (matching.getConnections().get(p)!=BipartiteMatching.FREE)
			  unfr1.add(p);
	  }
	  return unfr1;
  }
  
  private double logDensity() {
    double sum = 0.0;
    for (LogScaleFactor f : numericFactors)
      sum += f.logDensity();
    return sum;
  }
}
