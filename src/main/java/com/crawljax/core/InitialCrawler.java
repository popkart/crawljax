package com.crawljax.core;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.plugin.CrawljaxPluginsUtil;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateMachine;
import com.crawljax.core.state.StateVertix;

/**
 * This is the initial Crawler. An initial crawler crawls only the index page, creates the index
 * state and builds a session object and resumes the normal operations.
 * 
 * @author Stefan Lenselink <S.R.Lenselink@student.tudelft.nl>
 * @version $Id$
 */

public class InitialCrawler extends Crawler {

	private static final Logger LOGGER = Logger.getLogger(InitialCrawler.class);

	private final CrawljaxController controller;

	private EmbeddedBrowser browser; // should be final but try-catch prevents...

	private StateMachine stateMachine;

	/**
	 * The default constructor.
	 * 
	 * @param mother
	 *            the controller to use.
	 * @throws InterruptedException
	 */
	public InitialCrawler(CrawljaxController mother) {
		super(mother, new ArrayList<Eventable>(), "initial");
		try {
			browser = mother.getBrowserFactory().requestBrowser();
		} catch (InterruptedException e) {
			LOGGER.error("The request for a browser was interuped", e);
		}
		controller = mother;
	}

	@Override
	public EmbeddedBrowser getBrowser() {
		return browser;
	}

	@Override
	public StateMachine getStateMachine() {
		return stateMachine;
	}

	@Override
	public void run() {

		/**
		 * Go to the initial URL
		 */
		try {
			goToInitialURL();
		} catch (CrawljaxException e) {
			LOGGER.fatal("Failed to load the site: " + e.getMessage(), e);
		}

		/**
		 * Build the index state
		 */
		StateVertix indexState = null;
		try {
			indexState =
			        new StateVertix(this.getBrowser().getCurrentUrl(), "index", this.getBrowser()
			                .getDom(), controller.getStrippedDom(this.getBrowser()));
		} catch (CrawljaxException e) {
			LOGGER.error("Can not build the index state due to a CrawljaxException", e);
		}

		/**
		 * Build the StateFlowGraph
		 */
		StateFlowGraph stateFlowGraph = new StateFlowGraph(indexState);

		/**
		 * Build the StateMachine
		 */
		stateMachine =
		        new StateMachine(stateFlowGraph, indexState, controller.getInvariantList());

		/**
		 * Build the CrawlSession
		 */
		CrawlSession session =
		        new CrawlSession(controller.getBrowserFactory(), stateFlowGraph, indexState,
		                controller.getStartCrawl(), controller.getConfigurationReader());
		controller.setSession(session);

		/**
		 * Run OnNewState Plugins for the index state.
		 */
		CrawljaxPluginsUtil.runOnNewStatePlugins(session);

		/**
		 * The initial work is done, continue with the normal procedure!
		 */
		super.run();
	}
}