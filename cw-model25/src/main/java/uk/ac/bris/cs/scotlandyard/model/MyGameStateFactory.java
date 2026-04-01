package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {

		Objects.requireNonNull(setup);
		Objects.requireNonNull(mrX);
		Objects.requireNonNull(detectives);

		if (setup.moves.isEmpty()) throw new IllegalArgumentException("No rounds");
		if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Empty graph");

		if (mrX.isDetective()) throw new IllegalArgumentException("mrX must be MrX");
		if (detectives.isEmpty()) throw new IllegalArgumentException("Need detectives");

		java.util.Set<Piece> seenPieces = new java.util.HashSet<>();
		java.util.Set<Integer> seenLocations = new java.util.HashSet<>();

		for (Player d : detectives) {
			if (!d.isDetective())
				throw new IllegalArgumentException("All players in detectives must be detectives");

			if (!seenPieces.add(d.piece()))
				throw new IllegalArgumentException("Duplicate detective piece");

			if (!seenLocations.add(d.location()))
				throw new IllegalArgumentException("Duplicate detective location");

			if (d.tickets().getOrDefault(Ticket.SECRET, 0) > 0)
				throw new IllegalArgumentException("Detectives cannot have secret tickets");

			if (d.tickets().getOrDefault(Ticket.DOUBLE, 0) > 0)
				throw new IllegalArgumentException("Detectives cannot have double tickets");
		}

		return new MyGameState(
				setup,
				mrX,
				detectives,
				ImmutableSet.of(mrX.piece()),
				ImmutableList.of()
		);
	}

	private static final class MyGameState implements GameState {

		private final GameSetup setup;
		private final Player mrX;
		private final ImmutableList<Player> detectives;
		private final ImmutableSet<Piece> remaining;
		private final ImmutableList<LogEntry> log;

		private MyGameState(
				GameSetup setup,
				Player mrX,
				ImmutableList<Player> detectives,
				ImmutableSet<Piece> remaining,
				ImmutableList<LogEntry> log) {
			this.setup = setup;
			this.mrX = mrX;
			this.detectives = detectives;
			this.remaining = remaining;
			this.log = log;
		}

		@Nonnull @Override public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() {
			ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
			builder.add(mrX.piece());
			for (Player d : detectives) builder.add(d.piece());
			return builder.build();
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player d : detectives) {
				if (d.piece() == detective) return Optional.of(d.location());
			}
			return Optional.empty();
		}

		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (mrX.piece() == piece) {
				return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			}
			for (Player d : detectives) {
				if (d.piece() == piece) {
					return Optional.of(ticket -> d.tickets().getOrDefault(ticket, 0));
				}
			}
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}


		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			// 1. Detectives catch Mr X
			for (Player d : detectives) {
				if (d.location() == mrX.location()) {
					return detectives.stream()
							.map(Player::piece)
							.collect(ImmutableSet.toImmutableSet());
				}
			}

			// 2. Mr X wins if all detectives are stuck
			boolean anyDetectiveCanMove = false;
			for (Player d : detectives) {
				if (!makeSingleMoves(d).isEmpty()) {
					anyDetectiveCanMove = true;
					break;
				}
			}
			if (!anyDetectiveCanMove) {
				return ImmutableSet.of(mrX.piece());
			}

			// 3. Mr X wins if final round completed and not caught
			if (log.size() == setup.moves.size()) {
				return ImmutableSet.of(mrX.piece());
			}

			// 4. Detectives win if Mr X to move and has no legal move
			boolean mrXHasMoves = !makeSingleMoves(mrX).isEmpty() || !makeDoubleMoves(mrX).isEmpty();
			if (remaining.contains(mrX.piece()) && !mrXHasMoves) {
				return detectives.stream()
						.map(Player::piece)
						.collect(ImmutableSet.toImmutableSet());
			}

			return ImmutableSet.of();
		}


		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			if (!getWinner().isEmpty()) return ImmutableSet.of();

			ImmutableSet.Builder<Move> moves = ImmutableSet.builder();

			if (remaining.contains(mrX.piece())) {
				moves.addAll(makeSingleMoves(mrX));
				moves.addAll(makeDoubleMoves(mrX));
				return moves.build();
			}

			for (Player d : detectives) {
				if (remaining.contains(d.piece())) {
					moves.addAll(makeSingleMoves(d));
				}
			}
			return moves.build();
		}

		@Nonnull @Override public GameState advance(Move move) {
			if (move == null) throw new NullPointerException("move");
			if (!getAvailableMoves().contains(move))
				throw new IllegalArgumentException("Illegal move");

			if (move instanceof SingleMove sm) {
				if (sm.commencedBy().isMrX()) {
					Player newMrX = mrX.use(sm.ticket).at(sm.destination);

					LogEntry entry = setup.moves.get(log.size())
							? LogEntry.reveal(sm.ticket, sm.destination)
							: LogEntry.hidden(sm.ticket);

					ImmutableSet<Piece> nextRemaining = detectivesWhoCanMove(detectives);

					return new MyGameState(
							setup,
							newMrX,
							detectives,
							nextRemaining,
							ImmutableList.<LogEntry>builder().addAll(log).add(entry).build()
					);
				}

				// detective single move
				ImmutableList.Builder<Player> newDetectives = ImmutableList.builder();

				for (Player d : detectives) {
					if (d.piece() == sm.commencedBy()) {
						newDetectives.add(d.use(sm.ticket).at(sm.destination));
					} else {
						newDetectives.add(d);
					}
				}

				ImmutableList<Player> updatedDetectives = newDetectives.build();
				Player newMrX = mrX.give(sm.ticket);

				ImmutableSet<Piece> unmovedDetectives = remaining.stream()
						.filter(p -> p != sm.commencedBy())
						.collect(ImmutableSet.toImmutableSet());

                ImmutableSet<Piece> nextRemaining = detectivesWhoCanMove(updatedDetectives)
						.stream()
						.filter(unmovedDetectives::contains)
						.collect(ImmutableSet.toImmutableSet());
				if (nextRemaining.isEmpty() && log.size() < setup.moves.size()) {
					nextRemaining = ImmutableSet.of(mrX.piece());
				}

				return new MyGameState(
						setup,
						newMrX,
						updatedDetectives,
						nextRemaining,
						log
				);
			}

			if (move instanceof Move.DoubleMove dm) {
				Player newMrX = mrX.use(dm.ticket1).use(dm.ticket2).use(Ticket.DOUBLE).at(dm.destination2);

				LogEntry entry1 = setup.moves.get(log.size())
						? LogEntry.reveal(dm.ticket1, dm.destination1)
						: LogEntry.hidden(dm.ticket1);

				LogEntry entry2 = setup.moves.get(log.size() + 1)
						? LogEntry.reveal(dm.ticket2, dm.destination2)
						: LogEntry.hidden(dm.ticket2);

				ImmutableSet<Piece> nextRemaining = detectivesWhoCanMove(detectives);
				if (nextRemaining.isEmpty() && log.size() + 2 < setup.moves.size()) {
					nextRemaining = ImmutableSet.of(mrX.piece());
				}

				return new MyGameState(
						setup,
						newMrX,
						detectives,
						nextRemaining,
						ImmutableList.<LogEntry>builder()
								.addAll(log)
								.add(entry1)
								.add(entry2)
								.build()
				);
			}

			throw new IllegalArgumentException("Unknown move type");
		}

		private ImmutableSet<Move> makeSingleMoves(Player player) {
			return makeSingleMoves(player, detectives);
		}

		private ImmutableSet<Move> makeSingleMoves(Player player, ImmutableList<Player> detectiveList) {
			ImmutableSet.Builder<Move> moves = ImmutableSet.builder();

			for (int destination : setup.graph.adjacentNodes(player.location())) {

				if (isOccupiedByDetective(destination, detectiveList)) continue;

				for (Transport transport : Objects.requireNonNull(setup.graph.edgeValueOrDefault(
						player.location(), destination, ImmutableSet.of()))) {

					Ticket required = transport.requiredTicket();

					if (player.has(required)) {
						moves.add(new SingleMove(
								player.piece(),
								player.location(),
								required,
								destination));
					}

					if (player.isMrX() && player.has(Ticket.SECRET)) {
						moves.add(new SingleMove(
								player.piece(),
								player.location(),
								Ticket.SECRET,
								destination));
					}
				}
			}
			return moves.build();
		}

		private ImmutableSet<Move> makeDoubleMoves(Player player) {
			ImmutableSet.Builder<Move> moves = ImmutableSet.builder();

			if (!player.isMrX()) return moves.build();
			if (!player.has(Ticket.DOUBLE)) return moves.build();

			// Need two rounds left
			if (log.size() + 2 > setup.moves.size()) return moves.build();

			for (Move first : makeSingleMoves(player)) {
				SingleMove firstMove = (SingleMove) first;

				Player afterFirst = player.use(firstMove.ticket).at(firstMove.destination);

				for (Move second : makeSingleMoves(afterFirst)) {
					SingleMove secondMove = (SingleMove) second;

					moves.add(new Move.DoubleMove(
							player.piece(),
							player.location(),
							firstMove.ticket,
							firstMove.destination,
							secondMove.ticket,
							secondMove.destination));
				}
			}

			return moves.build();
		}

		private boolean isOccupiedByDetective(int location, ImmutableList<Player> detectiveList) {
			for (Player d : detectiveList) {
				if (d.location() == location) return true;
			}
			return false;
		}

		private ImmutableSet<Piece> detectivesWhoCanMove(ImmutableList<Player> detectiveList) {
			ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
			for (Player d : detectiveList) {
				if (!makeSingleMoves(d, detectiveList).isEmpty()) {
					builder.add(d.piece());
				}
			}
			return builder.build();
		}

	}
}
