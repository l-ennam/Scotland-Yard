package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.Piece.?; //possibly multiple imports.
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.?; //possibly multiple imports.
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives); 
	}


	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		
		
		private MyGameState(
		  	final GameSetup setup, 
		  	final ImmutableSet<Piece> remaining, 
		  	final ImmutableList<LogEntry> log,
		  	final Player mrX,
		  	final List<Player> detectives) {
			  	this.setup = setup;
			  	this.remaining = remaining;
			  	this.log = log;
			  	this.mrX = mrX;
			  	this.detectives = detectives;
			  	if (setup.moves.isEmpty())
				{
				  	throw new IllegalArgumentException("Error: Moves are empty.");
				}
				//TODO - Implement further
			}
			//TODO - Implement further
		}
		

		@Override public GameSetup getSetup() {
			return setup;
		}

		@Override public ImmutableList<LogEntry> getMrXTravelLog() { 
			return log; 
		}

		@Override public Optional<Integer> getDetectiveLocation(Detective detective){
		  // For all detectives, if Detective#piece == detective, then return the location in an Optional.of();
		  // otherwise, return Optional.empty();      
		}

		@Override public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		@Override public ImmutableSet<Piece> getWinner() {
			return winner;
		}


		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
		 
		  // TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
		  
		  for(int destination : setup.graph.adjacentNodes(source)) {
		    // TODO find out if destination is occupied by a detective
		    //  if the location is occupied, don't add to the collection of moves to return      
		    
		    for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
		      // TODO find out if the player has the required tickets
		      //  if it does, construct a SingleMove and add it the collection of moves to return  
		    }
		    
		    // TODO consider the rules of secret moves here
		    //  add moves to the destination via a secret ticket if there are any left with the player
		  }
		  
		  // TODO return the collection of moves
		}


		private static Set<Move> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log)
		{
			//TODO: Implement.
		}


		public GameState advance(Move move){
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			//TODO: Implement (probably the longest part of this)
		}
		

		//TODO: Helper Functions ...


		//TODO: Determining Winner Function...


		//TODO: Determining Moves Function...
}

