package com.langpack.dbprocess;

import java.util.ArrayList;

public class Worker implements Runnable {

	static int ARRAY_SIZE = 10;
	String name = null;
	static int currentTaskId = 0;

	public static ArrayList<String> queue = new ArrayList<>();

	public String getName() {
		return name;
	}

	public static void initializeQueue() {
		for (int i = 0; i < ARRAY_SIZE; i++) {
			queue.add("Task-" + i);
		}
	}

	public Worker(String tmp) {
		name = tmp;
	}

	public static void getTheKeyandProcess(Worker owner) {
		synchronized (queue) {
			try {
				if (currentTaskId < ARRAY_SIZE) {
					String label = owner.getName();
					Thread.currentThread();
					Thread.sleep(1000);
					String markAsDone = queue.get(currentTaskId) + "-" + label;
					queue.set(currentTaskId, markAsDone);
					System.out.println("Task completed : " + markAsDone);
					currentTaskId++;
				} else {
					System.out.println("All tasks completed !");
					for (int i = 0; i < ARRAY_SIZE; i++) {
						System.out.println("Task summary " + queue.get(i));
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		while (currentTaskId < ARRAY_SIZE) {
			System.out.println(">>>>>>>>>  " + this.getName());
			getTheKeyandProcess(this);
		}
	}

	public static void main(String[] args) {

		Worker.initializeQueue();
		// Worker myWorkerDelta = new Worker("DELTA");
		Worker myWorkerGamma = new Worker("GAMMA");
		Worker myWorkerBeta = new Worker("BETA");

		// Thread threadDelta = new Thread(myWorkerDelta);
		Thread threadGamma = new Thread(myWorkerGamma);
		Thread threadBeta = new Thread(myWorkerBeta);

		// threadDelta.start();
		threadGamma.start();
		threadBeta.start();

	}

}
