{-# LANGUAGE EmptyDataDecls, RankNTypes, ScopedTypeVariables #-}

module Slu_refimpl(Val, Rexp, lexer) where {

import Prelude ((==), (/=), (<), (<=), (>=), (>), (+), (-), (*), (/), (**),
  (>>=), (>>), (=<<), (&&), (||), (^), (^^), (.), ($), ($!), (++), (!!), Eq,
  error, id, return, not, fst, snd, map, filter, concat, concatMap, reverse,
  zip, null, takeWhile, dropWhile, all, any, Integer, negate, abs, divMod,
  String, Bool(True, False), Maybe(Nothing, Just));
import Data.Bits ((.&.), (.|.), (.^.));
import qualified Prelude;
import qualified Data.Bits;

data Nat = Zero_nat | Suc Nat;

data Set a = Set [a] | Coset [a];

data Char = Char Bool Bool Bool Bool Bool Bool Bool Bool;

data Val a = Void | Atm a | Seq (Val a) (Val a) | Right (Val a) | Left (Val a)
  | Stars [Val a] | Recv [Char] (Val a);

data Rexp a = Zero | One | Atom a | Plus (Rexp a) (Rexp a)
  | Times (Rexp a) (Rexp a) | Star (Rexp a) | NTimes (Rexp a) Nat
  | Upto (Rexp a) Nat | From (Rexp a) Nat | Rec [Char] (Rexp a)
  | Charset (Set a);

membera :: forall a. (Eq a) => [a] -> a -> Bool;
membera [] y = False;
membera (x : xs) y = x == y || membera xs y;

member :: forall a. (Eq a) => a -> Set a -> Bool;
member x (Coset xs) = not (membera xs x);
member x (Set xs) = membera xs x;

equal_nat :: Nat -> Nat -> Bool;
equal_nat Zero_nat (Suc x2) = False;
equal_nat (Suc x2) Zero_nat = False;
equal_nat (Suc x2) (Suc y2) = equal_nat x2 y2;
equal_nat Zero_nat Zero_nat = True;

nullable :: forall a. Rexp a -> Bool;
nullable Zero = False;
nullable One = True;
nullable (Atom c) = False;
nullable (Plus r1 r2) = nullable r1 || nullable r2;
nullable (Times r1 r2) = nullable r1 && nullable r2;
nullable (Star r) = True;
nullable (NTimes r n) = (if equal_nat n Zero_nat then True else nullable r);
nullable (Upto r n) = True;
nullable (From r n) = (if equal_nat n Zero_nat then True else nullable r);
nullable (Rec l r) = nullable r;
nullable (Charset cs) = False;

minus_nat :: Nat -> Nat -> Nat;
minus_nat (Suc m) (Suc n) = minus_nat m n;
minus_nat Zero_nat n = Zero_nat;
minus_nat m Zero_nat = m;

one_nat :: Nat;
one_nat = Suc Zero_nat;

deriv :: forall a. (Eq a) => a -> Rexp a -> Rexp a;
deriv c Zero = Zero;
deriv c One = Zero;
deriv ca (Atom c) = (if ca == c then One else Zero);
deriv c (Plus r1 r2) = Plus (deriv c r1) (deriv c r2);
deriv c (Times r1 r2) =
  (if nullable r1 then Plus (Times (deriv c r1) r2) (deriv c r2)
    else Times (deriv c r1) r2);
deriv c (Star r) = Times (deriv c r) (Star r);
deriv c (NTimes r n) =
  (if equal_nat n Zero_nat then Zero
    else Times (deriv c r) (NTimes r (minus_nat n one_nat)));
deriv c (Upto r n) =
  (if equal_nat n Zero_nat then Zero
    else Times (deriv c r) (Upto r (minus_nat n one_nat)));
deriv c (From r n) =
  (if equal_nat n Zero_nat then Times (deriv c r) (Star r)
    else Times (deriv c r) (From r (minus_nat n one_nat)));
deriv c (Rec l r) = deriv c r;
deriv c (Charset cs) = (if member c cs then One else Zero);

replicate :: forall a. Nat -> a -> [a];
replicate Zero_nat x = [];
replicate (Suc n) x = x : replicate n x;

mkeps :: forall a. Rexp a -> Val a;
mkeps One = Void;
mkeps (Times r1 r2) = Seq (mkeps r1) (mkeps r2);
mkeps (Plus r1 r2) =
  (if nullable r1 then Left (mkeps r1) else Right (mkeps r2));
mkeps (Star r) = Stars [];
mkeps (Upto r n) = Stars [];
mkeps (NTimes r n) = Stars (replicate n (mkeps r));
mkeps (From r n) = Stars (replicate n (mkeps r));
mkeps (Rec l r) = Recv l (mkeps r);

injval :: forall a. Rexp a -> a -> Val a -> Val a;
injval (Atom d) c Void = Atm c;
injval (Plus r1 r2) c (Left v1) = Left (injval r1 c v1);
injval (Plus r1 r2) c (Right v2) = Right (injval r2 c v2);
injval (Times r1 r2) c (Seq v1 v2) = Seq (injval r1 c v1) v2;
injval (Times r1 r2) c (Left (Seq v1 v2)) = Seq (injval r1 c v1) v2;
injval (Times r1 r2) c (Right v2) = Seq (mkeps r1) (injval r2 c v2);
injval (Star r) c (Seq v (Stars vs)) = Stars (injval r c v : vs);
injval (NTimes r n) c (Seq v (Stars vs)) = Stars (injval r c v : vs);
injval (Upto r n) c (Seq v (Stars vs)) = Stars (injval r c v : vs);
injval (From r n) c (Seq v (Stars vs)) = Stars (injval r c v : vs);
injval (Rec l r) c v = Recv l (injval r c v);
injval (Charset cs) c Void = Atm c;

lexer :: forall a. (Eq a) => Rexp a -> [a] -> Maybe (Val a);
lexer r [] = (if nullable r then Just (mkeps r) else Nothing);
lexer r (c : s) = (case lexer (deriv c r) s of {
                    Nothing -> Nothing;
                    Just v -> Just (injval r c v);
                  });

}
