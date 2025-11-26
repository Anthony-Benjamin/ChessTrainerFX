@startuml

skinparam classAttributeIconSize 0

' ===== MODELS =====
class BoardModel {
    - squares: SquareModel[][]
}

class SquareModel {
    - position: Position
    - piece: PieceModel
    + hasPiece(): boolean
}

class PieceModel {
    - type: String
    - color: String
}

class Position {
    - row: int
    - column: int
}

' ===== VIEWS =====
class BoardView {
    - squareViews: SquareView[][]
    + updateBoard()
}

class SquareView {
    - model: SquareModel
    - controller: Controller
    + updateView()
    + setOnClickListener()
}

' ===== CONTROLLER =====
class Controller {
    - selectedSquare: SquareModel
    + handleClick(SquareModel): void
    + attemptMove(from: SquareModel, to: SquareModel): void
}

' ===== MAIN =====
class ChesstrainerFX {
    + start()
}

' ===== RELATIONSHIPS =====
ChesstrainerFX --> Controller
ChesstrainerFX --> BoardModel
ChesstrainerFX --> BoardView

BoardModel --> SquareModel
BoardView --> SquareView
SquareView --> SquareModel
SquareView --> Controller
SquareModel --> PieceModel
SquareModel --> Position
Controller --> SquareModel

@enduml
