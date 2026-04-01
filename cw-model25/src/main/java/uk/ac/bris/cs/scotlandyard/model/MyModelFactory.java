package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.Event;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import java.util.List;
import java.util.ArrayList;
/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}
	private class MyModel implements Model
	{
		List<Observer> observerList = new ArrayList<>();
		Board.GameState state;

		public Board getCurrentBoard()
		{
			return state;
		}

		public ImmutableSet<Observer> getObservers ()
		{
			return ImmutableSet.copyOf(observerList);
		}

		MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives)
		{
			this.state = new MyGameStateFactory().build(setup, mrX, detectives);
		}

		public void chooseMove(@Nonnull Move move) 
		{
			Event obsEvent;
			state = state.advance(move);
			if (state.getWinner().isEmpty()) 
			{
				obsEvent = Event.MOVE_MADE;
			}
			else
			{ 
				obsEvent = Event.GAME_OVER;
			}
			List<Observer> obs = List.copyOf(observerList);
			int i = 0;
			while (i < obs.size()) 
			{
			    obs.get(i).onModelChanged(state, obsEvent);
			    i++;
			}
		}

		public void registerObserver(Observer observer)
		{
			if (observer == null) 
			{
				throw new NullPointerException("Error: Observer shouldnt be NULL");
			}
			else if (observerList.contains(observer)) 
			{
				throw new IllegalArgumentException("Error: Observer is already registered.");
			}
			else 
			{
				observerList.add(observer);
			}
		}
		
		public void unregisterObserver(Observer observer) {
			if (observer == null) 
			{
				throw new NullPointerException("Error: Observer shouldnt be NULL");
			}
			else if (!observerList.contains(observer) ) 
			{
				throw new IllegalArgumentException("Error: Observer isnt registered.");
			}
			else
			{ 
				observerList.remove(observer);
			}
		}
	}
}
