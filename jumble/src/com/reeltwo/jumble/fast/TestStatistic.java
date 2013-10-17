package com.reeltwo.jumble.fast;

/**
 * This class holds the mutation test result statistics.
 * 
 * @author Celia Lai
 * @version $Revision: 743 $
 */
public class TestStatistic {

  private int mStatus;

  private String mTime;

  public TestStatistic(int status, String time) {
    this.mStatus = status;
    this.mTime = time;
  }

  /**
   * Whether or not this test detected the mutation.
   * 
   * @return PASS (=0) means this test did detect the mutation.  Good!
   *         FAIL (=1) means the test passed, so did not detect the mutation.
   *         TIMEOUT (=2) means the test timed out.
   */
  public int getStatus() {
    return mStatus;
  }

  public void setStatus(int status) {
    this.mStatus = status;
  }

  public String getTime() {
    return mTime;
  }

  public void setTime(String time) {
    this.mTime = time;
  }
}
